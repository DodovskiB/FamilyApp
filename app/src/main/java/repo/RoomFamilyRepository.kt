package com.example.familyapp.data.repo

import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity

class RoomFamilyRepository(
    private val listDao: ListDao,
    private val itemDao: ItemDao
) : FamilyRepository {

    override suspend fun getLists(): List<ListEntity> {
        return listDao.getAll()
    }

    override suspend fun addList(name: String): Long {
        return listDao.insert(ListEntity(name = name))
    }

    override suspend fun getItems(listId: Long, kind: Int): List<ItemEntity> {
        return itemDao.getByList(listId, kind)
    }

    override suspend fun addItem(listId: Long, title: String, kind: Int): Long {
        return itemDao.insert(
            ItemEntity(
                listId = listId,
                title = title,
                isChecked = false,
                kind = kind
            )
        )
    }

    override suspend fun updateItem(item: ItemEntity) {
        itemDao.update(item)
    }
}
