package com.example.familyapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.sync.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository
import java.io.File
import java.util.UUID
import kotlin.random.Random

/** Only disposable databases and generated images; never reads a personal photo. */
@RunWith(AndroidJUnit4::class)
class SharedMediaStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun uuid() = UUID.randomUUID().toString()
    private suspend fun fixture(block: suspend (AppDatabase, RoomFamilyRepository, SyncStore, ItemMediaStore, Long, Long) -> Unit) {
        val name = "media-fixture-${uuid()}.db"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val ordinary = SyncStore(db); ordinary.prepare(); ordinary.seed()
            val media = ItemMediaStore(db); media.prepare()
            val repo = RoomFamilyRepository(db)
            val list = repo.addList("Fixture", 0); val item = repo.addItem(list, "Fixture entry", 0)
            block(db,repo,ordinary,media,list,item)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    private suspend fun row(sync: SyncStore, media: ItemMediaStore, item: Long, path: String? = null, link: String? = null, has: Boolean = true): JsonArray {
        val remote = sync.pending().single { it.json["entity"]?.jsonPrimitive?.content == "item" }.json.getValue("id").jsonPrimitive.content
        return buildJsonArray { add(buildJsonObject {
            put("id",remote); put("owner_id",uuid()); put("list_id",uuid()); put("has_media",has)
            put("photo_path",path?.let(::JsonPrimitive) ?: JsonNull); put("link_url",link?.let(::JsonPrimitive) ?: JsonNull)
        }) }
    }

    @Test fun independentFieldsAndStaleUploadAcknowledgement() = runBlocking { fixture { db,repo,sync,media,list,item ->
        repo.setItemLink(item,"https://example.com/local")
        db.itemDao().setPhoto(item,"${uuid()}.jpg")
        val first = media.pending()
        assertEquals(listOf(2,1),first.map { it.mask })
        val snapshot = row(sync,media,item,"remote/path.jpg","https://example.com/remote")
        media.apply(snapshot)
        assertEquals("https://example.com/local",repo.getItems(list,0).single().linkUrl)
        val replacement = "${uuid()}.jpg"
        db.itemDao().setPhoto(item,replacement)
        media.acknowledge(first.last(),"stale/path.jpg")
        assertNull(repo.getItems(list,0).single().photoRemotePath)
        assertEquals(replacement,repo.getItems(list,0).single().photoFileName)
        media.acknowledge(first.first(),null)
        media.apply(snapshot)
        assertEquals("https://example.com/remote",repo.getItems(list,0).single().linkUrl)
        assertEquals(replacement,repo.getItems(list,0).single().photoFileName)
    } }

    @Test fun authoritativeRemovalClearsLegacyPhotoAndKeepsPrivateCatalog() = runBlocking { fixture { db,repo,sync,media,list,item ->
        val local = "${uuid()}.jpg"
        db.itemDao().setPhoto(item,local)
        media.pending().forEach { media.acknowledge(it,null) }
        val product = repo.addCatalogItem("Private","Other")
        db.catalogItemDao().setPhoto(product,local)
        val released = media.apply(row(sync,media,item))
        assertEquals(listOf(local),released)
        assertNull(repo.getItems(list,0).single().photoFileName)
        assertEquals(local,db.catalogItemDao().getById(product)!!.photoFileName)
        assertTrue(media.referencedPhoto(local))
        assertEquals(0,media.count())
    } }

    @Test fun queueSurvivesReopenAndRemovedEntryIsArchived() = runBlocking {
        val name="media-durable-${uuid()}.db"
        var db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try {
            val sync=SyncStore(db); sync.prepare();sync.seed()
            var media=ItemMediaStore(db);media.prepare()
            var repo=RoomFamilyRepository(db)
            val list=repo.addList("Fixture",0);val item=repo.addItem(list,"Fixture",0)
            repo.setItemLink(item,"https://example.com/queued")
            val pending=media.pending()
            db.close();db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
            media=ItemMediaStore(db);media.prepare();repo=RoomFamilyRepository(db)
            assertEquals(pending,media.pending())
            db.itemDao().deleteByList(list)
            assertTrue(media.pending().isEmpty());assertEquals(0,media.count())
            db.openHelper.readableDatabase.query("SELECT count(*) FROM shopping_revoked_media").use { assertTrue(it.moveToFirst());assertEquals(1,it.getInt(0)) }
        } finally { db.close();context.deleteDatabase(name) }
    }

    @Test fun missingRemoteTargetDefersNewMediaInsteadOfLosingIt() = runBlocking { fixture { db,repo,_,media,_,item ->
        repo.setItemLink(item,"https://example.com/new")
        val fake=object:ItemMediaTransport {
            override suspend fun mediaSnapshot()=JsonArray(emptyList())
            override suspend fun applyMedia(change:MediaChange,target:JsonObject,photoPath:String?)=error("Unexpected upload")
            override suspend fun uploadPhoto(path:String,bytes:ByteArray)=error("Unexpected upload")
            override suspend fun downloadPhoto(path:String):ByteArray=error("Unexpected download")
            override suspend fun obsoletePhotos()=emptyList<String>()
            override suspend fun deletePhoto(path:String)=Unit
        }
        ItemMediaSynchronizer(db,fake,null).synchronize()
        assertEquals(1,media.pending().size)
    } }

    @Test fun repeatPurchaseAndOrdinaryEditsRetainSharedMedia() = runBlocking { fixture { db,repo,_,_,list,_ ->
        val product=repo.addCatalogItem("Repeat fixture","Other")
        val entry=repo.addCatalogItemToList(list,product)
        val stale=db.itemDao().getById(entry)!!
        repo.setItemLink(entry,"https://example.com/shared-edit")
        val path="${uuid()}/${uuid()}/${"a".repeat(64)}.jpg"
        db.itemDao().update(db.itemDao().getById(entry)!!.copy(photoRemotePath=path))
        repo.updateItem(stale.copy(title="Renamed"))
        assertEquals(path,db.itemDao().getById(entry)!!.photoRemotePath)
        assertEquals("https://example.com/shared-edit",db.itemDao().getById(entry)!!.linkUrl)
        repo.toggleChecked(entry,true)
        val next=repo.addCatalogItemToList(list,product,3)
        assertEquals(path,db.itemDao().getById(next)!!.photoRemotePath)
        assertEquals("https://example.com/shared-edit",db.itemDao().getById(next)!!.linkUrl)
        assertNull(db.catalogItemDao().getById(product)!!.linkUrl)
        repo.deleteCatalogItem(product)
        assertEquals(path,db.itemDao().getById(next)!!.photoRemotePath)
        assertNull(db.itemDao().getById(next)!!.catalogItemId)
    } }

    @Test fun migrationFromFiveBackfillsMediaWithoutChangingShoppingState() = runBlocking {
        val name="media-v5-${uuid()}.db";val path=context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            old.execSQL("CREATE TABLE lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE catalog_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,title TEXT NOT NULL,category TEXT NOT NULL,photoFileName TEXT,linkUrl TEXT)")
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,listId INTEGER NOT NULL,title TEXT NOT NULL,kind INTEGER NOT NULL,category TEXT NOT NULL,qty INTEGER NOT NULL,checked INTEGER NOT NULL,isActive INTEGER NOT NULL,catalogItemId INTEGER)")
            old.execSQL("INSERT INTO lists VALUES(1,'Home',0)")
            old.execSQL("INSERT INTO catalog_items VALUES(2,'Fixture','Other','aaaa-bbbb.jpg','https://example.com/legacy')")
            old.execSQL("INSERT INTO items VALUES(3,1,'Fixture',0,'Other',7,1,1,2)")
            old.execSQL("INSERT INTO items VALUES(4,1,'Unlinked',0,'Other',2,0,1,NULL)")
            old.version=5
        }
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_5_6).build()
        try {
            val item=db.itemDao().getById(3)!!
            assertEquals(7,item.qty);assertTrue(item.checked);assertEquals(2L,item.catalogItemId)
            assertEquals("aaaa-bbbb.jpg",item.photoFileName);assertEquals("https://example.com/legacy",item.linkUrl)
            assertNull(item.photoRemotePath);assertNull(db.itemDao().getById(4)!!.linkUrl)
        } finally { db.close();context.deleteDatabase(name) }
    }

    @Test fun compressionStripsMetadataAndCacheIsIsolatedAndRepairable() {
        val photos=ProductPhotoStore(context,"fixture-${uuid()}")
        val other=ProductPhotoStore(context,"fixture-${uuid()}")
        val source=File(context.cacheDir,"generated-${uuid()}.jpg")
        var imported:String?=null;var cached:String?=null;var otherCached:String?=null
        try {
            val bitmap=Bitmap.createBitmap(2400,1600,Bitmap.Config.ARGB_8888)
            val random=Random(41);val pixels=IntArray(2400*1600) { 0xff000000.toInt() or random.nextInt(0x1000000) }
            bitmap.setPixels(pixels,0,2400,0,0,2400,1600)
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)) };bitmap.recycle()
            ExifInterface(source.absolutePath).apply { setAttribute(ExifInterface.TAG_MAKE,"Generated fixture");setAttribute(ExifInterface.TAG_ORIENTATION,"6");saveAttributes() }
            val original=source.readBytes()
            imported=photos.importPhoto(Uri.fromFile(source))
            val bytes=photos.uploadBytes(imported)
            assertTrue(bytes.size<=ProductPhotoStore.MAX_BYTES)
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            assertTrue(bounds.outWidth in 1..1024 && bounds.outHeight in 1..1024)
            assertNull(ExifInterface(photos.file(imported).absolutePath).getAttribute(ExifInterface.TAG_MAKE))
            assertArrayEquals(original,source.readBytes());assertArrayEquals(bytes,photos.file(imported).readBytes())
            cached=photos.cache(bytes);otherCached=other.cache(bytes)
            assertNotEquals(cached,otherCached)
            photos.file(cached).writeBytes(byteArrayOf(0))
            assertEquals(cached,photos.cache(bytes));assertArrayEquals(bytes,photos.file(cached).readBytes())
            photos.delete(cached);assertTrue(other.file(otherCached).isFile)
        } finally { photos.delete(imported);photos.delete(cached);other.delete(otherCached);source.delete() }
    }
}
