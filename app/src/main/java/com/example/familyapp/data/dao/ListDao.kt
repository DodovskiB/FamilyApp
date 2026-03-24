package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Query("SELECT * FROM lists ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ListEntity): Long

    @Query("UPDATE lists SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteById(id: Long)
}
