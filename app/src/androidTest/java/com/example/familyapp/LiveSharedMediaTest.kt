package com.example.familyapp

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.auth.AccountRepository
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.sync.*
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository
import java.io.File
import java.util.UUID

/** Opt-in Storage API test. Generated images, isolated DBs and one disposable list only. */
@RunWith(AndroidJUnit4::class)
class LiveSharedMediaTest {
    @Test fun uploadsDownloadsOfflineEditsRemovalAndCleanupConverge() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("syncLive")=="true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val accounts=AccountRepository.getInstance(context)
        val session=withTimeout(30_000) { requireNotNull(accounts.status).filterIsInstance<SessionStatus.Authenticated>().first() }
        val remote=SupabaseSyncTransport(accounts,requireNotNull(session.session.user).id)
        val names=List(2) { "live-media-${UUID.randomUUID()}.db" }
        val dbs=names.map { Room.databaseBuilder(context,AppDatabase::class.java,it).build() }
        val photos=names.map { ProductPhotoStore(context,it) }
        val sync=dbs.indices.map { SyncController(dbs[it],remote,photos[it]) }
        val repo=dbs.indices.map { RoomFamilyRepository(dbs[it],photos[it]) }
        val source=File(context.cacheDir,"media-generated-${UUID.randomUUID()}.jpg")
        val title="Disposable media ${UUID.randomUUID()}"
        var remoteList:String?=null
        var uploaded:String?=null
        val imports=mutableSetOf<String>()
        try {
            val bitmap=Bitmap.createBitmap(120,80,Bitmap.Config.ARGB_8888).apply { eraseColor(0xff24ab68.toInt()) }
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,90,it) };bitmap.recycle()
            sync.forEach { it.synchronize() }
            val list=repo[0].addList(title,2001)
            remoteList=sync[0].store.remoteListId(list)
            val item=repo[0].addItem(list,"Generated media entry",0)
            repo[0].setItemLink(item,"https://example.com/media-fixture")
            repo[0].attachItemPhoto(item,Uri.fromFile(source))
            val original=repo[0].getItems(list,0).single().photoFileName!!;imports.add(original)
            sync[0].synchronize();sync[1].synchronize()
            val listB=repo[1].getLists().single { it.name==title }.id
            var b=repo[1].getItems(listB,0).single()
            uploaded=b.photoRemotePath
            assertNotNull(uploaded);assertNotNull(b.photoFileName)
            assertEquals("https://example.com/media-fixture",b.linkUrl)
            assertArrayEquals(photos[0].uploadBytes(original),photos[1].file(b.photoFileName!!).readBytes())
            assertNull(b.catalogItemId)
            // Same content is immutable and can be retried without a duplicate-object failure.
            remote.uploadPhoto(uploaded!!,photos[0].uploadBytes(original))
            // Remove only this fixture cache file: the next cycle must repair its DB reference.
            photos[1].delete(b.photoFileName);sync[1].synchronize()
            b=repo[1].getItems(listB,0).single();assertTrue(photos[1].file(b.photoFileName!!).isFile)
            // Offline independent edits: one side clears photo, the other changes only the URL.
            repo[0].removeItemPhoto(item)
            repo[1].setItemLink(b.id,"https://example.com/offline-change")
            sync[0].synchronize();sync[1].synchronize();sync[0].synchronize()
            for (i in 0..1) {
                val row=repo[i].getItems(if(i==0)list else listB,0).single()
                assertEquals("https://example.com/offline-change",row.linkUrl)
                assertNull(row.photoRemotePath);assertNull(row.photoFileName)
            }
            repo[1].setItemLink(b.id,"")
            sync[1].synchronize();sync[0].synchronize()
            assertNull(repo[0].getItems(list,0).single().linkUrl)
            assertEquals(0,ItemMediaStore(dbs[0]).count());assertEquals(0,ItemMediaStore(dbs[1]).count())
        } finally {
            try {
                remoteList?.let { id -> remote.apply(buildJsonArray { add(buildJsonObject {
                    put("operation_id",UUID.randomUUID().toString());put("entity","list");put("id",id)
                    put("action","delete");put("body",buildJsonObject {})
                }) }) }
                // Deleted fixture list allows immediate object cleanup, without touching live lists.
                uploaded?.let {
                    assertTrue(remote.obsoletePhotos().contains(it))
                    remote.deletePhoto(it)
                    assertFalse(remote.obsoletePhotos().contains(it))
                }
            } finally {
                imports.forEach { photos[0].delete(it) }
                photos.forEach { store -> store.cachedNames().forEach(store::delete) }
                dbs.forEach { it.close() };names.forEach { context.deleteDatabase(it) };source.delete()
            }
        }
    }
}
