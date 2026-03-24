package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.familyapp.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE listId = :listId AND isActive = 1 ORDER BY id ASC")
    fun observeByList(listId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE listId = :listId AND isActive = 1 ORDER BY id ASC")
    suspend fun getByList(listId: Long): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ItemEntity): Long

    @Query("UPDATE items SET qty = :qty WHERE id = :id")
    suspend fun setQty(id: Long, qty: Int)

    @Query("UPDATE items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE items SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}
