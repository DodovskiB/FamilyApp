package com.example.familyapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.sync.SyncStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import repo.PurchasedProductNeedsConfirmation
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class PurchasedStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun clearingPurchasedIsScopedDurableAndQueuesOnlyEntryChanges() = runBlocking {
        val name = "purchased-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            var repo = RoomFamilyRepository(db)
            val home = repo.addList("Home", 0); val other = repo.addList("Other", 1)
            val milk = repo.addCatalogItem("Milk", "Dairy"); val bread = repo.addCatalogItem("Bread", "Bakery")
            val bought = repo.addCatalogItemToList(home, milk)
            val outstanding = repo.addCatalogItemToList(home, bread)
            val otherEntry = repo.addCatalogItemToList(other, milk)
            repo.toggleChecked(bought, true); repo.toggleChecked(otherEntry, true)
            val sync = SyncStore(db); sync.prepare(); sync.seed()
            sync.acknowledge(sync.pending().map { it.operationId })
            repo.clearPurchased(home)
            assertEquals(listOf(outstanding), repo.getItems(home, 0).map { it.id })
            assertTrue(repo.getItems(other, 0).single().checked)
            assertEquals(2, repo.observeCatalogItems().first().size)
            val queued = sync.pending().single().json
            assertEquals("item", queued.getValue("entity").jsonPrimitive.content)
            assertEquals(buildJsonObject { put("active", false) }, queued.getValue("body"))
            repo.clearPurchased(home)
            assertEquals(1, sync.pendingCount())
            db.close(); db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            repo = RoomFamilyRepository(db)
            assertEquals(listOf(outstanding), repo.getItems(home, 0).map { it.id })
            assertEquals(2, repo.observeCatalogItems().first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun buyingAgainRequiresConfirmationAndStartsNewQuantityAndIdentity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = RoomFamilyRepository(db)
            val list = repo.addList("Home", 0); val product = repo.addCatalogItem("Milk", "Dairy")
            val old = repo.addCatalogItemToList(list, product)
            repo.setQty(old, 7); repo.toggleChecked(old, true)
            try { repo.addCatalogItemToList(list, product); fail("Confirmation required") }
            catch (expected: PurchasedProductNeedsConfirmation) { assertEquals(product, expected.productId) }
            assertEquals(7, repo.getItems(list, 0).single().qty)
            try { repo.addCatalogItemToList(list, product, 0); fail("Invalid quantity") } catch (_: IllegalArgumentException) { }
            val next = repo.addCatalogItemToList(list, product, 2)
            assertNotEquals(old, next)
            val active = repo.getItems(list, 0).single()
            assertEquals(2, active.qty); assertFalse(active.checked)
            assertEquals(product, active.catalogItemId)
            assertEquals(next, repo.addCatalogItemToList(list, product))
            assertEquals(3, repo.getItems(list, 0).single().qty)
            assertEquals(1, repo.observeCatalogItems().first().size)
        } finally { db.close() }
    }
}
