package com.example.familyapp.data.repo

import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

class RoomFamilyRepository(
    private val listDao: ListDao,
    private val itemDao: ItemDao
) {
    fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()

    fun observeItems(listId: Long): Flow<List<ItemEntity>> =
        itemDao.observeByList(listId)

    suspend fun addList(name: String, sortOrder: Int? = null): Long {
        val order = sortOrder ?: (listDao.getMaxSortOrder() + 1)
        return listDao.insert(ListEntity(name = name.trim(), sortOrder = order))
    }

    suspend fun addOrIncrementItem(listId: Long, title: String): Long {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return -1

        val existing = itemDao.findActiveByTitle(listId, trimmed.lowercase())
        return if (existing != null) {
            itemDao.update(existing.copy(quantity = existing.quantity + 1))
            existing.id
        } else {
            itemDao.insert(
                ItemEntity(
                    listId = listId,
                    title = trimmed,
                    quantity = 1,
                    isChecked = false,
                    isActive = true
                )
            )
        }
    }

    suspend fun updateItem(item: ItemEntity) = itemDao.update(item)

    suspend fun deactivateItem(item: ItemEntity) =
        itemDao.update(item.copy(isActive = false))
}
