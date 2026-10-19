package com.example.familyapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class CatalogStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun openDatabase(name: String) = Room.databaseBuilder(
        context, AppDatabase::class.java, name
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).build()

    private fun repository(db: AppDatabase) = RoomFamilyRepository(db)

    @Test
    fun catalogChangesPersistAcrossDatabaseReopen() = runBlocking {
        val name = "catalog-test-${System.nanoTime()}.db"
        var db = openDatabase(name)
        try {
            val repo = repository(db)
            val milkId = repo.addCatalogItem(" Milk ", " Dairy ")
            val breadId = repo.addCatalogItem("Bread", "")
            val initial = withTimeout(5_000) { repo.observeCatalogItems().first() }
            assertEquals("Milk", initial.first { it.id == milkId }.title)
            assertEquals("Dairy", initial.first { it.id == milkId }.category)
            assertEquals("Other", initial.first { it.id == breadId }.category)
            val milk = initial.first { it.id == milkId }
            repo.updateCatalogItem(milk.copy(title = " Oat milk ", category = " Drinks "))
            repo.deleteCatalogItem(breadId)
            db.close()
            db = openDatabase(name)
            val saved = withTimeout(5_000) { repository(db).observeCatalogItems().first() }
            assertEquals(1, saved.size)
            assertEquals(milkId, saved.single().id)
            assertEquals("Oat milk", saved.single().title)
            assertEquals("Drinks", saved.single().category)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationKeepsExistingListsAndItems() = runBlocking {
        val name = "migration-test-${System.nanoTime()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, listId INTEGER NOT NULL, title TEXT NOT NULL, kind INTEGER NOT NULL, category TEXT NOT NULL, qty INTEGER NOT NULL, checked INTEGER NOT NULL, isActive INTEGER NOT NULL)")
            old.execSQL("INSERT INTO lists VALUES (1, 'Home', 0)")
            old.execSQL("INSERT INTO items VALUES (1, 1, 'Milk', 0, 'Dairy', 2, 1, 1)")
            old.version = 1
        }
        val db = openDatabase(name)
        try {
            assertEquals("Home", db.listDao().getAll().single().name)
            val item = db.itemDao().getByList(1).single()
            assertEquals("Milk", item.title)
            assertEquals(2, item.qty)
            assertTrue(item.checked)
            val repo = repository(db)
            assertTrue(withTimeout(5_000) { repo.observeCatalogItems().first() }.isEmpty())
            repo.addCatalogItem("Bread", "Bakery")
            assertEquals("Bread", withTimeout(5_000) { repo.observeCatalogItems().first() }.single().title)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
