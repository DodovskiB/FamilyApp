package com.example.familyapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,

    val title: String,

    // tap во Shopping list го менува ова (прецртано/избледено)
    val isChecked: Boolean = false,

    // stepper (+/-). Ако стане 0 -> item се трга (isActive=false)
    val quantity: Int = 1,

    // Категорија за боја/групирање (подоцна ќе додадеме UI за избор)
    val category: String = "Разно",

    // Само URI/path, не слика во база
    val imageUri: String? = null,

    val note: String? = null,

    // Soft delete
    val isActive: Boolean = true
)
