package com.example.familyapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.sync.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class SharingStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val listId = UUID.randomUUID().toString()
    private val itemId = UUID.randomUUID().toString()
    private val ownerId = UUID.randomUUID().toString()
    private fun row(entity: String, id: String, body: JsonObject) = buildJsonObject {
        put("entity", entity); put("id", id); put("body", body); put("deleted", false); put("revision", 9000)
    }
    private fun snapshot(allowed: Boolean = true) = buildJsonObject {
        put("access", if (!allowed) JsonArray(emptyList()) else buildJsonArray { add(buildJsonObject {
            put("id", listId); put("owner_id", ownerId); put("role", "admin")
        }) })
        put("records", if (!allowed) JsonArray(emptyList()) else buildJsonArray {
            add(row("list", listId, buildJsonObject { put("name", "Shared fixture"); put("sort_order", 0) }))
            add(row("item", itemId, buildJsonObject {
                put("list_id", listId); put("catalog_id", JsonNull); put("title", "Fixture"); put("category", "Other")
                put("qty", 2); put("checked", false); put("active", true)
            }))
        })
        put("invitations", JsonArray(emptyList())); put("members", JsonArray(emptyList()))
    }
    private inner class Transport : SyncTransport, SharingTransport, ItemMediaTransport {
        var allowed = true
        var unavailable = false
        val personal = mutableListOf<JsonObject>()
        val shared = mutableListOf<JsonObject>()
        var mediaWrites = 0
        override suspend fun mediaSnapshot() = if (!allowed) JsonArray(emptyList()) else buildJsonArray { add(buildJsonObject {
            put("id",itemId);put("owner_id",ownerId);put("list_id",listId);put("has_media",false)
            put("link_url",JsonNull);put("photo_path",JsonNull)
        }) }
        override suspend fun applyMedia(change: MediaChange,target: JsonObject,photoPath: String?) { mediaWrites++ }
        override suspend fun uploadPhoto(path: String,bytes: ByteArray) = error("No fixture image should be uploaded")
        override suspend fun downloadPhoto(path: String): ByteArray = error("No fixture image should be downloaded")
        override suspend fun obsoletePhotos() = emptyList<String>()
        override suspend fun deletePhoto(path: String) = Unit
        override suspend fun pull(afterRevision: Long) = JsonArray(emptyList())
        override suspend fun apply(operations: JsonArray) { personal.addAll(operations.map { it.jsonObject }) }
        override suspend fun snapshot(): JsonObject {
            if (unavailable) error("Offline")
            val base = snapshot(allowed)
            return if (!allowed) base else JsonObject(base + ("records" to JsonArray(
                base.getValue("records").jsonArray + shared.filter { it["action"]?.jsonPrimitive?.content == "insert" }.map {
                    row("item", it.getValue("id").jsonPrimitive.content,
                        JsonObject(it.getValue("body").jsonObject + ("catalog_id" to JsonNull)))
                }
            )))
        }
        override suspend fun applyShared(owner: String, list: String, operations: JsonArray) {
            assertEquals(ownerId, owner); assertEquals(listId, list)
            // Match server prevalidation: an update cannot refer to an entry whose
            // insert has not committed in an earlier request.
            val existing = shared.filter { it["action"]?.jsonPrimitive?.content == "insert" }
                .map { it.getValue("id").jsonPrimitive.content }.toSet() + itemId
            operations.forEach {
                val op = it.jsonObject
                if (op.getValue("action").jsonPrimitive.content != "insert")
                    check(op.getValue("id").jsonPrimitive.content in existing) { "Entry not committed" }
            }
            shared.addAll(operations.map { it.jsonObject })
        }
        override suspend fun invite(list: String, email: String) = Unit
        override suspend fun respond(invitation: String, accept: Boolean) = Unit
        override suspend fun remove(list: String, member: String?, invitation: String?) = Unit
    }
    private fun db() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Test fun newSharedEntryCommitsBeforeItsQueuedEdits() = runBlocking {
        val db = db()
        try {
            val remote = Transport()
            val sync = SyncController(db, remote)
            sync.synchronize()
            val repo = RoomFamilyRepository(db)
            val list = repo.getLists().single()
            val product = repo.addCatalogItem("Rapid fixture", "Other")
            repo.addCatalogItemToList(list.id, product)
            val entry = repo.getItems(list.id, 0).single { it.title == "Rapid fixture" }
            repo.changeQty(entry.id, 1)
            repo.toggleChecked(entry.id, true)
            sync.synchronize()
            assertEquals(listOf("insert", "update", "update"), remote.shared.map { it.getValue("action").jsonPrimitive.content })
            assertEquals(0, sync.store.pendingCount())
            assertEquals("catalog", remote.personal.single().getValue("entity").jsonPrimitive.content)
        } finally { db.close() }
    }

    @Test fun revokedMediaIsArchivedAndNeverUploadedUnderPersonalOwnership() = runBlocking {
        val db=db()
        try {
            val remote=Transport();val sync=SyncController(db,remote);sync.synchronize()
            val repo=RoomFamilyRepository(db);val list=repo.getLists().single()
            val entry=repo.getItems(list.id,0).single()
            repo.setItemLink(entry.id,"https://example.com/offline")
            db.itemDao().setPhoto(entry.id,"${UUID.randomUUID()}.jpg")
            remote.unavailable=true
            try { sync.synchronize();fail("Offline capture lost") } catch (_: IllegalStateException) { }
            assertEquals(2,ItemMediaStore(db).count())
            remote.unavailable=false;remote.allowed=false
            sync.synchronize()
            assertTrue(repo.getLists().isEmpty());assertEquals(0,remote.mediaWrites)
            assertTrue(remote.personal.isEmpty());assertEquals(0,ItemMediaStore(db).count())
            db.openHelper.readableDatabase.query("SELECT count(*) FROM shopping_revoked_media").use { it.moveToFirst();assertEquals(2,it.getInt(0)) }
        } finally { db.close() }
    }

    @Test fun sharedChangesKeepOwnerIdentityAndDoNotAdvancePersonalCursor() = runBlocking {
        val db = db()
        try {
            val transport = Transport()
            val controller = SyncController(db, transport)
            controller.synchronize()
            assertEquals(0L, controller.store.cursor())
            val repo = RoomFamilyRepository(db)
            val list = repo.getLists().single()
            val entry = repo.getItems(list.id, 0).single()
            repo.changeQty(entry.id, 1); repo.toggleChecked(entry.id, true)
            val product = repo.addCatalogItem("Personal fixture", "Other")
            repo.addCatalogItemToList(list.id, product)
            controller.synchronize()
            assertEquals(3, transport.shared.size)
            assertEquals("catalog", transport.personal.single().getValue("entity").jsonPrimitive.content)
            assertEquals(0, controller.store.pendingCount())
            assertEquals(product, repo.getItems(list.id, 0).single { it.title == "Personal fixture" }.catalogItemId)
            val reopened = SyncController(db, transport)
            reopened.restoreSharingAccess()
            assertEquals("admin", reopened.sharing.value.access.getValue(list.id).role)
            try { repo.deleteList(list.id); fail("Admin deletion must be blocked locally") } catch (_: IllegalStateException) { }
            assertEquals(1, repo.getLists().size)
        } finally { db.close() }
    }

    @Test fun revocationQuarantinesOfflineWorkWithoutUploadingItAsPersonalData() = runBlocking {
        val db = db()
        try {
            val transport = Transport()
            val controller = SyncController(db, transport)
            controller.synchronize()
            val repo = RoomFamilyRepository(db)
            val sharedList = repo.getLists().single()
            val entry = repo.getItems(sharedList.id, 0).single()
            repo.changeQty(entry.id, 1); repo.toggleChecked(entry.id, true)
            transport.unavailable = true
            try { controller.synchronize(); fail("Offline cycle should not acknowledge") } catch (_: IllegalStateException) { }
            assertEquals(2, controller.store.pendingCount())
            assertEquals(1, repo.getLists().size)
            transport.unavailable = false; transport.allowed = false
            val personal = repo.addList("Private retained", 0)
            controller.synchronize()
            assertEquals(listOf(personal), repo.getLists().map { it.id })
            assertTrue(transport.shared.isEmpty())
            assertEquals("list", transport.personal.single().getValue("entity").jsonPrimitive.content)
            assertEquals(0, controller.store.pendingCount())
            db.openHelper.readableDatabase.query("SELECT count(*) FROM shopping_revoked_changes").use {
                it.moveToFirst(); assertEquals(2, it.getInt(0))
            }
            assertTrue(controller.sharing.value.message!!.contains("offline changes"))
        } finally { db.close() }
    }
}
