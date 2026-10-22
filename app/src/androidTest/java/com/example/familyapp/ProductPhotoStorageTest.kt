package com.example.familyapp

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.photo.ProductPhotoStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProductPhotoStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6).build()

    @Test
    fun photoCopiesPersistReplaceSafelyAndAreCleanedUp() = runBlocking {
        val name = "photos-${System.nanoTime()}.db"
        val store = ProductPhotoStore(context)
        var db = open(name)
        val source = File.createTempFile("test-photo-", ".jpg", context.cacheDir)
        val invalid = File.createTempFile("bad-photo-", ".txt", context.cacheDir)
        val imported = mutableSetOf<String>()
        try {
            val bitmap = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            ExifInterface(source.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            var repo = RoomFamilyRepository(db, store)
            val id = repo.addCatalogItem("Milk", "Dairy")
            val stale = db.catalogItemDao().getById(id)!!
            repo.attachCatalogPhoto(id, Uri.fromFile(source))
            val first = db.catalogItemDao().getById(id)!!.photoFileName!!
            imported += first
            assertTrue(store.file(first).exists())
            val normalized = store.thumbnail(first, 1600)!!
            assertTrue(normalized.height > normalized.width)
            assertTrue(normalized.height <= 1600)
            normalized.recycle()
            // A text edit using a stale catalog snapshot must not erase the attached photo.
            repo.updateCatalogItem(stale.copy(title = "Fresh milk"))
            assertEquals(first, db.catalogItemDao().getById(id)!!.photoFileName)
            db.close()
            db = open(name)
            repo = RoomFamilyRepository(db, store)
            assertEquals(first, db.catalogItemDao().getById(id)!!.photoFileName)
            invalid.writeText("not an image")
            try {
                repo.attachCatalogPhoto(id, Uri.fromFile(invalid))
                fail("Invalid image should be rejected")
            } catch (_: Exception) {
                assertEquals(first, db.catalogItemDao().getById(id)!!.photoFileName)
                assertTrue(store.file(first).exists())
            }
            repo.attachCatalogPhoto(id, Uri.fromFile(source))
            val second = db.catalogItemDao().getById(id)!!.photoFileName!!
            imported += second
            assertNotEquals(first, second)
            assertFalse(store.file(first).exists())
            repo.removeCatalogPhoto(id)
            assertNull(db.catalogItemDao().getById(id)!!.photoFileName)
            assertFalse(store.file(second).exists())
            assertTrue(source.exists())
            repo.attachCatalogPhoto(id, Uri.fromFile(source))
            val third = db.catalogItemDao().getById(id)!!.photoFileName!!
            imported += third
            repo.deleteCatalogItem(id)
            assertFalse(store.file(third).exists())
        } finally {
            db.close()
            context.deleteDatabase(name)
            source.delete()
            invalid.delete()
            imported.forEach(store::delete)
        }
    }

    @Test
    fun versionThreeMigrationKeepsItemsAndTranslatesOnlyKnownListNames() = runBlocking {
        val name = "v3-photo-${System.nanoTime()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, listId INTEGER NOT NULL, title TEXT NOT NULL, kind INTEGER NOT NULL, category TEXT NOT NULL, qty INTEGER NOT NULL, checked INTEGER NOT NULL, isActive INTEGER NOT NULL, catalogItemId INTEGER)")
            old.execSQL("CREATE TABLE catalog_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL)")
            old.execSQL("INSERT INTO lists VALUES (1, 'Doma', 0), (2, 'Vikendica', 0), (3, 'Odmor', 0), (4, 'My custom list', 0)")
            old.execSQL("INSERT INTO catalog_items VALUES (1, 'Milk', 'Dairy')")
            old.execSQL("INSERT INTO items VALUES (1, 1, 'Milk', 0, 'Dairy', 3, 1, 1, 1)")
            old.version = 3
        }
        val db = open(name)
        try {
            assertEquals(listOf("Home", "Weekend House", "Vacation", "My custom list"), db.listDao().getAll().map { it.name })
            val product = db.catalogItemDao().getById(1)!!
            assertEquals("Milk", product.title)
            assertNull(product.photoFileName)
            val entry = db.itemDao().getByList(1).single()
            assertEquals(3, entry.qty)
            assertTrue(entry.checked)
            assertEquals(1L, entry.catalogItemId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
