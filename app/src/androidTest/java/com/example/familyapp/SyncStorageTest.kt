package com.example.familyapp

import androidx.room.Room
import androidx.room.withTransaction
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
class SyncStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
    private fun uuid() = UUID.randomUUID().toString()
    private fun record(entity: String, id: String, revision: Long, body: JsonObject, deleted: Boolean = false) = buildJsonObject {
        put("entity", entity); put("id", id); put("revision", revision); put("body", body); put("deleted", deleted)
    }

    @Test fun queuedChangesAreAtomicAndSurviveReopen() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        var db = open(name)
        try {
            var store = SyncStore(db)
            store.prepare(); store.seed()
            val repo = RoomFamilyRepository(db)
            val list = repo.addList("Test", 0)
            val product = repo.addCatalogItem("Milk", "Dairy")
            val item = repo.addCatalogItemToList(list, product)
            assertEquals(listOf("list", "catalog", "item"), store.pending().map { it.json.getValue("entity").jsonPrimitive.content })
            val before = store.pending().size
            try {
                db.withTransaction { repo.changeQty(item, 1); error("Rollback") }
            } catch (_: IllegalStateException) { }
            assertEquals(before, store.pending().size)
            assertEquals(1, repo.getItems(list, 0).single().qty)
            repo.changeQty(item, 1)
            repo.toggleChecked(item, true)
            val queued = store.pending()
            assertEquals(1L, queued[3].json.getValue("body").jsonObject.getValue("qty_delta").jsonPrimitive.long)
            assertFalse(queued[3].json.getValue("body").jsonObject.containsKey("checked"))
            db.close(); db = open(name); store = SyncStore(db); store.prepare()
            assertEquals(queued, store.pending())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun remoteRowsApplyInDependencyOrderWithoutEchoAndPreservePendingEdits() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        val db = open(name)
        try {
            val store = SyncStore(db); store.prepare(); store.seed()
            val list = uuid(); val product = uuid(); val item = uuid()
            val listRow = record("list", list, 30, buildJsonObject { put("name", "Remote"); put("sort_order", 0) })
            val productRow = record("catalog", product, 20, buildJsonObject {
                put("title", "Milk"); put("category", "Dairy"); put("link_url", "https://example.com/milk")
            })
            val itemRow = record("item", item, 10, buildJsonObject {
                put("title", "Milk"); put("category", "Dairy"); put("qty", 2); put("checked", false)
                put("active", true); put("list_id", list); put("catalog_id", product)
            })
            store.apply(JsonArray(listOf(itemRow, productRow, listRow)))
            val repo = RoomFamilyRepository(db)
            val localList = repo.getLists().single().id
            val localItem = repo.getItems(localList, 0).single()
            assertEquals(2, localItem.qty)
            assertNotNull(localItem.catalogItemId)
            assertEquals(0, store.pendingCount())
            repo.toggleChecked(localItem.id, true)
            val deletion = record("item", item, 40, buildJsonObject {}, true)
            store.apply(JsonArray(listOf(deletion)))
            assertTrue(repo.getItems(localList, 0).single().checked)
            assertEquals(30L, store.cursor())
            store.acknowledge(store.pending().map { it.operationId })
            store.apply(JsonArray(listOf(deletion)))
            assertTrue(repo.getItems(localList, 0).isEmpty())
            assertNotNull(db.catalogItemDao().getById(localItem.catalogItemId!!))
            assertEquals(40L, store.cursor())
            assertEquals(0, store.pendingCount())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun failedUploadRetainsSameOperationForRetryAndDoesNotLoseEditsDuringFirstDownload() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            val list = repo.addList("Offline", 0)
            var failUpload = true
            val received = mutableListOf<JsonArray>()
            val remote = object : SyncTransport {
                override suspend fun pull(afterRevision: Long): JsonArray {
                    if (received.isEmpty()) repo.renameList(list, "Edited during first download")
                    return JsonArray(emptyList())
                }
                override suspend fun apply(operations: JsonArray) {
                    received.add(operations)
                    if (failUpload) throw java.io.IOException("Offline")
                }
            }
            val controller = SyncController(db, remote)
            try { controller.synchronize(); fail("Expected offline failure") } catch (_: java.io.IOException) { }
            assertEquals(2, controller.store.pendingCount())
            failUpload = false
            controller.synchronize()
            assertEquals(received[0], received[1])
            assertEquals(0, controller.store.pendingCount())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun matchingImportsRequireChoiceAndPreserveAllItemStateWhenCombined() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            val list = repo.addList("Home", 0)
            val item = repo.addItem(list, "Local product", 0)
            repo.setQty(item, 4); repo.toggleChecked(item, true)
            val remoteId = uuid()
            val rows = JsonArray(listOf(record("list", remoteId, 1, buildJsonObject { put("name", "Home"); put("sort_order", 0) })))
            val uploaded = mutableListOf<JsonObject>()
            val controller = SyncController(db, object : SyncTransport {
                override suspend fun pull(afterRevision: Long) = if (afterRevision == 0L) rows else JsonArray(emptyList())
                override suspend fun apply(operations: JsonArray) { uploaded.addAll(operations.map { it.jsonObject }) }
            })
            controller.synchronize()
            assertTrue(controller.status.value.needsImportChoice)
            assertTrue(uploaded.isEmpty())
            assertFalse(controller.store.hasInitialDownload())
            controller.chooseImport(true)
            controller.synchronize()
            assertFalse(controller.status.value.needsImportChoice)
            assertEquals(1, repo.getLists().size)
            assertEquals(4, repo.getItems(list, 0).single().qty)
            assertTrue(repo.getItems(list, 0).single().checked)
            assertEquals("item", uploaded.single().getValue("entity").jsonPrimitive.content)
            assertEquals(remoteId, uploaded.single().getValue("body").jsonObject.getValue("list_id").jsonPrimitive.content)
            controller.synchronize()
            assertEquals(1, uploaded.size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun ambiguousMatchingNamesAreNotAutomaticallyCombined() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            repo.addList("Home", 0); repo.addList("Home", 1)
            val store = SyncStore(db); store.prepare(); store.seed()
            val rows = JsonArray(listOf(record("list", uuid(), 1, buildJsonObject { put("name", "Home"); put("sort_order", 0) })))
            assertTrue(store.matchingInitialLists(rows).isEmpty())
            assertEquals(2, store.pendingCount())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun separateImportChoiceRetainsIndependentSameNameLists() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            repo.addList("Home", 0)
            val rows = JsonArray(listOf(record("list", uuid(), 1, buildJsonObject { put("name", "Home"); put("sort_order", 0) })))
            val controller = SyncController(db, object : SyncTransport {
                override suspend fun pull(afterRevision: Long) = if (afterRevision == 0L) rows else JsonArray(emptyList())
                override suspend fun apply(operations: JsonArray) { }
            })
            controller.synchronize(); controller.chooseImport(false); controller.synchronize()
            assertEquals(2, repo.getLists().size)
            assertFalse(controller.status.value.needsImportChoice)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun untouchedDefaultsCanBeReplacedButEditedListsArePreserved() = runBlocking {
        val name = "sync-test-${uuid()}.db"
        val db = open(name)
        try {
            val repo = RoomFamilyRepository(db)
            listOf("Home", "Weekend House", "Vacation").forEachIndexed { index, title -> repo.addList(title, index) }
            val store = SyncStore(db); store.prepare(); store.seed()
            store.discardPristineDefaults()
            assertTrue(repo.getLists().isEmpty())
            assertEquals(0, store.pendingCount())
            repo.addList("My own list", 0)
            store.discardPristineDefaults()
            assertEquals("My own list", repo.getLists().single().name)
            assertEquals(1, store.pendingCount())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
