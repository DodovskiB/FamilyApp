package com.example.familyapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["listId"]),
        Index(value = ["kind"]),
        Index(value = ["isActive"]),
        Index(value = ["category"])
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // 0 = глобален каталог (само кога kind=1), иначе реален listId
    @ColumnInfo(name = "listId")
    val listId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    // 0 = Shopping List (активен список), 1 = Shopping Items (каталог)
    @ColumnInfo(name = "kind")
    val kind: Int,

    // Категорија (за боја/групирање)
    @ColumnInfo(name = "category")
    val category: String = "Other",

    @ColumnInfo(name = "qty")
    val qty: Int = 1,

    @ColumnInfo(name = "checked")
    val checked: Boolean = false,

    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true,

    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int = 0
)
