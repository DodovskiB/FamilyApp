package repo

import androidx.room.withTransaction
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.CatalogItemEntity
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

class RoomFamilyRepository(private val db: AppDatabase) : FamilyRepository {
    private val listDao = db.listDao()
    private val itemDao = db.itemDao()
    private val catalogItemDao = db.catalogItemDao()

    override fun observeCatalogItems(): Flow<List<CatalogItemEntity>> = catalogItemDao.observeAll()

    override suspend fun addCatalogItem(title: String, category: String): Long {
        require(title.isNotBlank()) { "Product name is required" }
        return catalogItemDao.insert(
            CatalogItemEntity(title = title.trim(), category = category.trim().ifBlank { "Other" })
        )
    }

    override suspend fun updateCatalogItem(item: CatalogItemEntity) {
        require(item.title.isNotBlank()) { "Product name is required" }
        catalogItemDao.update(
            item.copy(title = item.title.trim(), category = item.category.trim().ifBlank { "Other" })
        )
    }

    override suspend fun deleteCatalogItem(id: Long) = db.withTransaction {
        // Existing list entries retain their name, quantity and purchased state.
        itemDao.clearCatalogReference(id)
        catalogItemDao.deleteById(id)
    }

    override suspend fun addCatalogItemToList(listId: Long, catalogItemId: Long): Long =
        db.withTransaction {
            requireNotNull(listDao.getById(listId)) { "List no longer exists" }
            val product = requireNotNull(catalogItemDao.getById(catalogItemId)) { "Product no longer exists" }
            val existing = itemDao.findActiveCatalogItem(listId, catalogItemId)
            if (existing != null) {
                itemDao.changeQty(existing.id, 1)
                existing.id
            } else {
                itemDao.insert(
                    ItemEntity(
                        listId = listId, title = product.title, kind = 0,
                        category = product.category, qty = 1, checked = false,
                        isActive = true, catalogItemId = product.id
                    )
                )
            }
        }

    override suspend fun createProductForList(listId: Long, title: String, category: String): Long =
        db.withTransaction {
            requireNotNull(listDao.getById(listId)) { "List no longer exists" }
            val productId = addCatalogItem(title, category)
            addCatalogItemToList(listId, productId)
        }

    override fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()
    override suspend fun getLists(): List<ListEntity> = listDao.getAll()

    override suspend fun addList(name: String, sortOrder: Int): Long {
        require(name.isNotBlank()) { "List name is required" }
        return listDao.insert(ListEntity(name = name.trim(), sortOrder = sortOrder))
    }

    override suspend fun renameList(listId: Long, newName: String) {
        require(newName.isNotBlank()) { "List name is required" }
        listDao.rename(listId, newName.trim())
    }

    override suspend fun deleteList(listId: Long) = db.withTransaction {
        itemDao.deleteByList(listId)
        listDao.deleteById(listId)
    }

    override fun observeItems(listId: Long, kind: Int): Flow<List<ItemEntity>> =
        itemDao.observeByList(listId, kind)

    override suspend fun getItems(listId: Long, kind: Int): List<ItemEntity> =
        itemDao.getByList(listId, kind)

    override suspend fun addItem(listId: Long, title: String, kind: Int): Long = db.withTransaction {
        requireNotNull(listDao.getById(listId)) { "List no longer exists" }
        require(title.isNotBlank()) { "Product name is required" }
        itemDao.insert(
            ItemEntity(listId = listId, title = title.trim(), kind = kind,
                category = "Other", qty = 1, checked = false, isActive = true)
        )
    }

    override suspend fun updateItem(item: ItemEntity) {
        require(item.qty >= 1 && item.title.isNotBlank())
        itemDao.update(item)
    }

    override suspend fun setQty(id: Long, qty: Int) {
        require(qty >= 1)
        itemDao.setQty(id, qty)
    }

    override suspend fun changeQty(id: Long, delta: Int) {
        require(delta == 1 || delta == -1)
        itemDao.changeQty(id, delta)
    }

    override suspend fun toggleChecked(id: Long, checked: Boolean) = itemDao.setChecked(id, checked)
    override suspend fun deleteItemSoft(id: Long) = itemDao.setActive(id, false)
}
