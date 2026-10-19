package com.example.familyapp

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.DefaultShoppingLists
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultShoppingListsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyDatabaseGetsDefaultsOnceAndDeletedListsStayDeleted() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val prefsName = "default-lists-test-${System.nanoTime()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        try {
            DefaultShoppingLists.initialize(db, prefs)
            val initial = db.listDao().getAll()
            assertEquals(listOf("Home", "Weekend House", "Vacation"), initial.map { it.name })
            DefaultShoppingLists.initialize(db, prefs)
            assertEquals(initial, db.listDao().getAll())
            initial.forEach { db.listDao().deleteById(it.id) }
            DefaultShoppingLists.initialize(db, prefs)
            assertTrue(db.listDao().getAll().isEmpty())
        } finally { db.close(); context.deleteSharedPreferences(prefsName) }
    }

    @Test
    fun existingListsArePreservedWithoutAddingDefaults() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val prefsName = "existing-lists-test-${System.nanoTime()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        try {
            val id = db.listDao().insert(ListEntity(name = "My list", sortOrder = 0))
            DefaultShoppingLists.initialize(db, prefs)
            assertEquals(listOf(ListEntity(id, "My list", 0)), db.listDao().getAll())
        } finally { db.close(); context.deleteSharedPreferences(prefsName) }
    }
}
