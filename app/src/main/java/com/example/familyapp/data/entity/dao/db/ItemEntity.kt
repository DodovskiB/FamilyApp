package com.example.familyapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val isChecked: Boolean = false,
    val kind: Int = 0,          // 0=Shopping list, 1=Shopping items
    val isActive: Boolean = true // ако е false, item е “избришан” од каталог
)
