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

    // за прикажување во UI (само активни)
    @Query("SELECT * FROM items WHERE listId = :listId AND kind = :kind AND isActive = 1 ORDER BY id DESC")
    fun observeByList(listId: Long, kind: Int): Flow<List<ItemEntity>>
}
