package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.familyapp.data.entity.ListEntity

@Dao
interface ListDao {

    @Insert
    suspend fun insert(list: ListEntity): Long

    @Query("SELECT * FROM lists ORDER BY name ASC")
    suspend fun getAll(): List<ListEntity>
}