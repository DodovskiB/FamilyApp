package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Insert
    suspend fun insert(list: ListEntity): Long

    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM lists")
    suspend fun getMaxSortOrder(): Int
}
