package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.familyapp.data.entity.CatalogItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemDao {
    @Query("SELECT * FROM catalog_items ORDER BY category COLLATE NOCASE, title COLLATE NOCASE, id")
    fun observeAll(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getById(id: Long): CatalogItemEntity?

    @Query("UPDATE catalog_items SET title = :title, category = :category WHERE id = :id")
    suspend fun updateDetails(id: Long, title: String, category: String)

    @Query("UPDATE catalog_items SET photoFileName = :fileName WHERE id = :id")
    suspend fun setPhoto(id: Long, fileName: String?)

    @Insert
    suspend fun insert(item: CatalogItemEntity): Long

    @Update
    suspend fun update(item: CatalogItemEntity)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
