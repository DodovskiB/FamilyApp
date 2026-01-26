package com.example.familyapp.data.repo

import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

class RoomFamilyRepository(
    private val listDao: ListDao,
    private val itemDao: ItemDao
) : FamilyRepository {

    fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()

    fun observeItems(listId: Long, kind: Int): Flow<List<ItemEntity>> =
        itemDao.observeByList(listId, kind)

    override suspend fun addList(name: String): Long =
        listDao.insert(ListEntity(name = name))

    override suspend fun addItem(listId: Long, title: String, kind: Int): Long =
        itemDao.insert(
            ItemEntity(
                listId = listId,
                title = title,
                isChecked = false,
                kind = kind,
                isActive = true
            )
        )

    override suspend fun updateItem(item: ItemEntity) {
        itemDao.update(item)
    }

    // Soft delete (catalog delete)
    suspend fun deactivateItem(item: ItemEntity) {
        itemDao.update(item.copy(isActive = false))
    }

    // (интерфејс legacy)
    override suspend fun getLists(): List<ListEntity> = emptyList()
    override suspend fun getItems(listId: Long, kind: Int): List<ItemEntity> = emptyList()
}
