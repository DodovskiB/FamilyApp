package com.example.familyapp.data.dao

import androidx.room.*
import com.example.familyapp.data.entity.ItemEntity

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE listId = :listId AND kind = :kind ORDER BY id ASC")
    suspend fun getByList(listId: Long, kind: Int): List<ItemEntity>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)
}
