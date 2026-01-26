package com.example.familyapp.data.repo

import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity

interface FamilyRepository {
    suspend fun getLists(): List<ListEntity>
    suspend fun addList(name: String): Long

    // Items се врзани за listId + kind (0/1)
    suspend fun getItems(listId: Long, kind: Int): List<ItemEntity>
    suspend fun addItem(listId: Long, title: String, kind: Int): Long
    suspend fun updateItem(item: ItemEntity)
}
