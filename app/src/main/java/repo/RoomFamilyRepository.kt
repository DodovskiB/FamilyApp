package com.example.familyapp.data.repo

import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

private const val KIND_LIST = 0
private const val KIND_CATALOG = 1
private const val CATALOG_LIST_ID = 0L

class RoomFamilyRepository(
    private val listDao: ListDao,
    private val itemDao: ItemDao
) : FamilyRepository {

    override suspend fun getLists(): List<ListEntity> = listDao.getAll()

    override fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()

    override suspend fun addList(name: String, sortOrder: Int): Long {
        return listDao.insert(ListEntity(name = name.trim(), sortOrder = sortOrder))
    }

    override fun observeItems(listId: Long, kind: Int): Flow<List<ItemEntity>> {
        return if (kind == KIND_CATALOG) itemDao.observeCatalog()
        else itemDao.observeShoppingList(listId)
    }

    override suspend fun getItems(listId: Long, kind: Int): List<ItemEntity> {
        return if (kind == KIND_CATALOG) itemDao.getCatalog()
        else itemDao.getShoppingList(listId)
    }

    override suspend fun addItem(listId: Long, title: String, kind: Int): Long {
        val clean = title.trim()
        if (clean.isBlank()) return -1L

        return if (kind == KIND_CATALOG) {
            addCatalogIfMissing(clean, category = "Other")
        } else {
            addToShoppingList(listId, clean)
        }
    }

    private suspend fun addCatalogIfMissing(title: String, category: String): Long {
        val existing = itemDao.findActiveInCatalog(title)
        if (existing != null) return existing.id

        return itemDao.insert(
            ItemEntity(
                listId = CATALOG_LIST_ID,
                title = title,
                kind = KIND_CATALOG,
                category = category,
                qty = 1,
                checked = false,
                isActive = true
            )
        )
    }

    private suspend fun addToShoppingList(listId: Long, title: String): Long {
        val cat = itemDao.findActiveInCatalog(title) ?: run {
            val id = addCatalogIfMissing(title, category = "Other")
            itemDao.findActiveInCatalog(title) ?: return id
        }

        val existingInList = itemDao.findActiveInShoppingList(listId, title)
        return if (existingInList != null) {
            itemDao.setQty(existingInList.id, existingInList.qty + 1)
            existingInList.id
        } else {
            itemDao.insert(
                ItemEntity(
                    listId = listId,
                    title = title,
                    kind = KIND_LIST,
                    category = cat.category,
                    qty = 1,
                    checked = false,
                    isActive = true
                )
            )
        }
    }

    override suspend fun updateItem(item: ItemEntity) = itemDao.update(item)

    override suspend fun setQty(id: Long, qty: Int) {
        itemDao.setQty(id, qty)
        if (qty <= 0) itemDao.setActive(id, false)
    }

    override suspend fun toggleChecked(id: Long, checked: Boolean) =
        itemDao.setChecked(id, checked)

    override suspend fun deleteItemSoft(id: Long) =
        itemDao.setActive(id, false)
}
