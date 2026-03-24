package com.example.familyapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,

    // Needed because your errors said these were missing:
    val kind: Int,
    val category: String,
    val qty: Int,
    val checked: Boolean,
    val isActive: Boolean
)
