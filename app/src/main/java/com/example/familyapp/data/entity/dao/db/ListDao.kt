package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Query("SELECT * FROM lists ORDER BY name ASC")
    fun observeAll(): Flow<List<ListEntity>>

    @Insert
    suspend fun insert(list: ListEntity): Long
}
