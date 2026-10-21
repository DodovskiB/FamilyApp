package com.example.familyapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.familyapp.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE listId = :listId AND kind = :kind AND isActive = 1 ORDER BY id ASC")
    fun observeByList(listId: Long, kind: Int = 0): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE listId = :listId AND kind = :kind AND isActive = 1 ORDER BY id ASC")
    suspend fun getByList(listId: Long, kind: Int = 0): List<ItemEntity>

    @Query("SELECT * FROM items WHERE listId = :listId AND catalogItemId = :catalogItemId AND kind = 0 AND isActive = 1 LIMIT 1")
    suspend fun findActiveCatalogItem(listId: Long, catalogItemId: Long): ItemEntity?

    @Query("UPDATE items SET qty = MIN(2147483647, MAX(1, qty + :delta)) WHERE id = :id AND isActive = 1")
    suspend fun changeQty(id: Long, delta: Int)

    @Query("DELETE FROM items WHERE listId = :listId")
    suspend fun deleteByList(listId: Long)

    @Query("UPDATE items SET catalogItemId = NULL WHERE catalogItemId = :catalogItemId")
    suspend fun clearCatalogReference(catalogItemId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ItemEntity): Long

    @Update
    suspend fun update(entity: ItemEntity)

    @Query("UPDATE items SET qty = :qty WHERE id = :id")
    suspend fun setQty(id: Long, qty: Int)

    @Query("UPDATE items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE items SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("UPDATE items SET isActive = 0 WHERE listId = :listId AND kind = 0 AND isActive = 1 AND checked = 1")
    suspend fun clearPurchased(listId: Long)
}
