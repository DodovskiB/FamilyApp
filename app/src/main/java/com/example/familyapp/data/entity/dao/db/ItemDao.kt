package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.familyapp.data.entity.ItemEntity

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY id DESC")
    suspend fun getByList(listId: Long): List<ItemEntity>

    @Update
    suspend fun update(item: ItemEntity)
}
