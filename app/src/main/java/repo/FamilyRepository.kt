package com.example.familyapp.data.repo

import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity

interface FamilyRepository {

    // ===== LISTS =====
    suspend fun getLists(): List<ListEntity>
    suspend fun addList(name: String): Long

    // ===== ITEMS =====
    suspend fun getItems(listId: Long): List<ItemEntity>
    suspend fun addItem(listId: Long, title: String): Long
    suspend fun updateItem(item: ItemEntity)
}
