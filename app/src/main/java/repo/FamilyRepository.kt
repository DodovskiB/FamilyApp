package com.example.familyapp.data.repo

import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

interface FamilyRepository {

    suspend fun getLists(): List<ListEntity>
    fun observeLists(): Flow<List<ListEntity>>
    suspend fun addList(name: String, sortOrder: Int = 0): Long

    fun observeItems(listId: Long, kind: Int): Flow<List<ItemEntity>>
    suspend fun getItems(listId: Long, kind: Int): List<ItemEntity>

    suspend fun addItem(listId: Long, title: String, kind: Int): Long
    suspend fun updateItem(item: ItemEntity)

    suspend fun setQty(id: Long, qty: Int)
    suspend fun toggleChecked(id: Long, checked: Boolean)
    suspend fun deleteItemSoft(id: Long)
}
