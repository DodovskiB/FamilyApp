package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.familyapp.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("SELECT * FROM items WHERE listId = :listId AND isActive = 1 ORDER BY id DESC")
    fun observeByList(listId: Long): Flow<List<ItemEntity>>

    // за да не се дуплира (case-insensitive)
    @Query(
        "SELECT * FROM items " +
                "WHERE listId = :listId AND isActive = 1 AND lower(title) = :titleLower " +
                "LIMIT 1"
    )
    suspend fun findActiveByTitle(listId: Long, titleLower: String): ItemEntity?
}
