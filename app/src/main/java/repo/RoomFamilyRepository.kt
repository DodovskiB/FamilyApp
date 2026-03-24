package repo

import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

class RoomFamilyRepository(
    private val listDao: ListDao,
    private val itemDao: ItemDao
) : FamilyRepository {

    /* ---------- LISTS ---------- */

    override fun observeLists(): Flow<List<ListEntity>> =
        listDao.observeAll()

    override suspend fun getLists(): List<ListEntity> =
        listDao.getAll()

    override suspend fun addList(
        name: String,
        sortOrder: Int
    ): Long {
        return listDao.insert(
            ListEntity(
                name = name,
                sortOrder = sortOrder
            )
        )
    }

    override suspend fun renameList(
        listId: Long,
        newName: String
    ) {
        listDao.rename(
            id = listId,
            newName = newName
        )
    }

    override suspend fun deleteList(
        listId: Long
    ) {
        listDao.deleteById(listId)
    }

    /* ---------- ITEMS ---------- */

    override fun observeItems(
        listId: Long,
        kind: Int
    ): Flow<List<ItemEntity>> =
        itemDao.observeByList(listId)

    override suspend fun getItems(
        listId: Long,
        kind: Int
    ): List<ItemEntity> =
        itemDao.getByList(listId)

    override suspend fun addItem(
        listId: Long,
        title: String,
        kind: Int
    ): Long {
        return itemDao.insert(
            ItemEntity(
                listId = listId,
                title = title,
                kind = kind,
                category = "Other",
                qty = 1,
                checked = false,
                isActive = true
            )
        )
    }

    override suspend fun updateItem(
        item: ItemEntity
    ) {
        itemDao.update(item)
    }

    override suspend fun setQty(
        id: Long,
        qty: Int
    ) {
        itemDao.setQty(id, qty)
    }

    override suspend fun toggleChecked(
        id: Long,
        checked: Boolean
    ) {
        itemDao.setChecked(id, checked)
    }

    override suspend fun deleteItemSoft(
        id: Long
    ) {
        itemDao.setActive(id, false)
    }
}
