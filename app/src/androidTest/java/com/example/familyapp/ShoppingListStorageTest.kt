package com.example.familyapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class ShoppingListStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
    private suspend fun catalog(repo: RoomFamilyRepository) = withTimeout(5_000) { repo.observeCatalogItems().first() }

    @Test
    fun independentListsKeepSnapshotsAndSurviveReopen() = runBlocking {
        val name = "lists-${System.nanoTime()}.db"
        var db = open(name)
        try {
            var repo = RoomFamilyRepository(db)
            val home = repo.addList("Home", 0)
            val travel = repo.addList("Travel", 0)
            val milk = repo.addCatalogItem("Milk", "Dairy")
            val homeItem = repo.addCatalogItemToList(home, milk)
            assertEquals(homeItem, repo.addCatalogItemToList(home, milk))
            val travelItem = repo.addCatalogItemToList(travel, milk)
            assertEquals(2, repo.getItems(home, 0).single().qty)
            assertEquals(1, repo.getItems(travel, 0).single().qty)
            repo.changeQty(homeItem, 1)
            repo.toggleChecked(homeItem, true)
            assertFalse(repo.getItems(travel, 0).single().checked)
            assertEquals(1, repo.getItems(travel, 0).single().qty)
            repo.updateCatalogItem(catalog(repo).single().copy(title = "Oat milk", category = "Drinks"))
            assertEquals("Milk", repo.getItems(home, 0).single().title)
            assertEquals("Dairy", repo.getItems(home, 0).single().category)
            repo.deleteCatalogItem(milk)
            assertTrue(catalog(repo).isEmpty())
            val retained = repo.getItems(home, 0).single()
            assertEquals("Milk", retained.title)
            assertEquals(3, retained.qty)
            assertTrue(retained.checked)
            assertNull(retained.catalogItemId)
            repo.deleteItemSoft(homeItem)
            assertTrue(repo.getItems(home, 0).isEmpty())
            assertEquals(travelItem, repo.getItems(travel, 0).single().id)
            repo.renameList(travel, " Holiday ")
            repo.deleteList(home)
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM items WHERE listId = $home").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
            db.close()
            db = open(name)
            repo = RoomFamilyRepository(db)
            assertEquals("Holiday", repo.getLists().single().name)
            assertEquals("Milk", repo.getItems(travel, 0).single().title)
            assertEquals(1, repo.getItems(travel, 0).single().qty)
            assertFalse(repo.getItems(travel, 0).single().checked)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun concurrentAddsStayInOneRowAndQuantityHasBounds() = runBlocking {
        val name = "quantity-${System.nanoTime()}.db"
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            val list = repo.addList("Home", 0)
            val product = repo.addCatalogItem("Bread", "")
            coroutineScope { repeat(10) { launch { repo.addCatalogItemToList(list, product) } } }
            val entry = repo.getItems(list, 0).single()
            assertEquals(10, entry.qty)
            repeat(12) { repo.changeQty(entry.id, -1) }
            assertEquals(1, repo.getItems(list, 0).single().qty)
            repo.setQty(entry.id, Int.MAX_VALUE)
            repo.changeQty(entry.id, 1)
            assertEquals(Int.MAX_VALUE, repo.getItems(list, 0).single().qty)
            repo.deleteItemSoft(entry.id)
            val replacement = repo.addCatalogItemToList(list, product)
            assertNotEquals(entry.id, replacement)
            assertEquals(1, repo.getItems(list, 0).single().qty)
            val newEntry = repo.createProductForList(list, " Eggs ", "")
            assertEquals("Eggs", repo.getItems(list, 0).first { it.id == newEntry }.title)
            assertEquals(2, catalog(repo).size)
            try {
                repo.createProductForList(-1, "Should not be saved", "")
                fail("Adding to a missing list should fail")
            } catch (_: IllegalArgumentException) {
                assertEquals(2, catalog(repo).size)
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionTwoMigrationPreservesCatalogAndUnlinkedEntries() = runBlocking {
        val name = "v2-${System.nanoTime()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, listId INTEGER NOT NULL, title TEXT NOT NULL, kind INTEGER NOT NULL, category TEXT NOT NULL, qty INTEGER NOT NULL, checked INTEGER NOT NULL, isActive INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE catalog_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL)")
            old.execSQL("INSERT INTO lists VALUES (1, 'Home', 0)")
            old.execSQL("INSERT INTO items VALUES (1, 1, 'Legacy item', 0, 'Other', 4, 1, 1)")
            old.execSQL("INSERT INTO catalog_items VALUES (1, 'Milk', 'Dairy')")
            old.version = 2
        }
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            val legacy = repo.getItems(1, 0).single()
            assertEquals("Legacy item", legacy.title)
            assertEquals(4, legacy.qty)
            assertTrue(legacy.checked)
            assertNull(legacy.catalogItemId)
            assertEquals("Milk", catalog(repo).single().title)
            repo.addCatalogItemToList(1, 1)
            assertEquals(2, repo.getItems(1, 0).size)
            repo.deleteList(1)
            assertEquals("Milk", catalog(repo).single().title)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
