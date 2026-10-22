package com.example.familyapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class ProductLinkStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build()

    @Test
    fun linksValidatePersistAndSurviveIndependentTextEdits() = runBlocking {
        val name = "links-${System.nanoTime()}.db"
        var db = open(name)
        try {
            var repo = RoomFamilyRepository(db)
            val id = repo.addCatalogItem("Milk", "Dairy")
            val stale = db.catalogItemDao().getById(id)!!
            repo.setCatalogLink(id, " www.amazon.com/dp/example?tag=test ")
            val amazon = "https://www.amazon.com/dp/example?tag=test"
            assertEquals(amazon, db.catalogItemDao().getById(id)!!.linkUrl)
            repo.updateCatalogItem(stale.copy(title = "Fresh milk"))
            assertEquals(amazon, db.catalogItemDao().getById(id)!!.linkUrl)
            for (invalid in listOf("javascript:alert(1)", "ftp://example.com/file", "https://", "not a link")) {
                try { repo.setCatalogLink(id, invalid); fail("Invalid URL accepted") }
                catch (_: Exception) { assertEquals(amazon, db.catalogItemDao().getById(id)!!.linkUrl) }
            }
            db.close()
            db = open(name)
            repo = RoomFamilyRepository(db)
            assertEquals(amazon, db.catalogItemDao().getById(id)!!.linkUrl)
            repo.setCatalogLink(id, "https://www.ebay.com/itm/example")
            assertEquals("https://www.ebay.com/itm/example", db.catalogItemDao().getById(id)!!.linkUrl)
            repo.setCatalogLink(id, "")
            assertNull(db.catalogItemDao().getById(id)!!.linkUrl)
            assertEquals("Fresh milk", db.catalogItemDao().getById(id)!!.title)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test
    fun migrationFromFourKeepsPhotoAndShoppingState() = runBlocking {
        val name = "v4-link-${System.nanoTime()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, listId INTEGER NOT NULL, title TEXT NOT NULL, kind INTEGER NOT NULL, category TEXT NOT NULL, qty INTEGER NOT NULL, checked INTEGER NOT NULL, isActive INTEGER NOT NULL, catalogItemId INTEGER)")
            old.execSQL("CREATE TABLE catalog_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, photoFileName TEXT)")
            old.execSQL("INSERT INTO lists VALUES (1, 'Home', 0)")
            old.execSQL("INSERT INTO catalog_items VALUES (1, 'Milk', 'Dairy', '1111-2222.jpg')")
            old.execSQL("INSERT INTO items VALUES (1, 1, 'Milk', 0, 'Dairy', 3, 1, 1, 1)")
            old.version = 4
        }
        val db = open(name)
        try {
            assertEquals("1111-2222.jpg", db.catalogItemDao().getById(1)!!.photoFileName)
            assertNull(db.catalogItemDao().getById(1)!!.linkUrl)
            assertEquals(3, db.itemDao().getByList(1).single().qty)
            assertTrue(db.itemDao().getByList(1).single().checked)
            RoomFamilyRepository(db).setCatalogLink(1, "https://example.com/product")
            assertEquals("1111-2222.jpg", db.catalogItemDao().getById(1)!!.photoFileName)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
