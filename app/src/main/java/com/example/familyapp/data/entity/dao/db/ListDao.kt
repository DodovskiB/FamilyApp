package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(list: ListEntity): Long

    // ✅ ОВА го бара RoomFamilyRepository (getAll)
    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ListEntity>

    // ✅ ОВА го бара MainMenuViewModel (observeLists)
    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ListEntity>>
}
