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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    // ===== Shopping List (kind=0) =====
    @Query("""
        SELECT * FROM items
        WHERE listId = :listId AND kind = 0 AND isActive = 1
        ORDER BY sortOrder ASC, title COLLATE NOCASE ASC
    """)
    fun observeShoppingList(listId: Long): Flow<List<ItemEntity>>

    @Query("""
        SELECT * FROM items
        WHERE listId = :listId AND kind = 0 AND isActive = 1
        ORDER BY sortOrder ASC, title COLLATE NOCASE ASC
    """)
    suspend fun getShoppingList(listId: Long): List<ItemEntity>

    @Query("""
        SELECT * FROM items
        WHERE listId = :listId AND kind = 0 AND isActive = 1
          AND LOWER(title) = LOWER(:title)
        LIMIT 1
    """)
    suspend fun findActiveInShoppingList(listId: Long, title: String): ItemEntity?


    // ===== Global Catalog (kind=1, listId=0) =====
    @Query("""
        SELECT * FROM items
        WHERE listId = 0 AND kind = 1 AND isActive = 1
        ORDER BY category COLLATE NOCASE ASC, title COLLATE NOCASE ASC
    """)
    fun observeCatalog(): Flow<List<ItemEntity>>

    @Query("""
        SELECT * FROM items
        WHERE listId = 0 AND kind = 1 AND isActive = 1
        ORDER BY category COLLATE NOCASE ASC, title COLLATE NOCASE ASC
    """)
    suspend fun getCatalog(): List<ItemEntity>

    @Query("""
        SELECT * FROM items
        WHERE listId = 0 AND kind = 1 AND isActive = 1
          AND LOWER(title) = LOWER(:title)
        LIMIT 1
    """)
    suspend fun findActiveInCatalog(title: String): ItemEntity?

    // Suggestions (за autocomplete) – враќа до 20
    @Query("""
        SELECT * FROM items
        WHERE listId = 0 AND kind = 1 AND isActive = 1
          AND LOWER(title) LIKE '%' || LOWER(:q) || '%'
        ORDER BY title COLLATE NOCASE ASC
        LIMIT 20
    """)
    suspend fun searchCatalog(q: String): List<ItemEntity>


    // ===== updates =====
    @Query("UPDATE items SET qty = :qty WHERE id = :id")
    suspend fun setQty(id: Long, qty: Int)

    @Query("UPDATE items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("UPDATE items SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE items SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE items SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: String)
}
