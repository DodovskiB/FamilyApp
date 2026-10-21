package com.example.familyapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.auth.AccountRepository
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.sync.SyncController
import com.example.familyapp.sync.SupabaseSyncTransport
import io.github.jan.supabase.auth.status.SessionStatus
import java.util.UUID
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

/** Opt-in only: exercises two isolated local databases against the signed-in account.
 * Creates a uniquely named disposable list and removes only that list afterward.
 * Never opens or modifies the user's real local Room database. */
@RunWith(AndroidJUnit4::class)
class LiveSyncTest {
    @Test fun purchasedCyclesAndClearConvergeWithoutDeletingCatalog() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("syncLive") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accounts = AccountRepository.getInstance(context)
        val session = withTimeout(30_000) { requireNotNull(accounts.status).filterIsInstance<SessionStatus.Authenticated>().first() }
        val remote = SupabaseSyncTransport(accounts, requireNotNull(session.session.user).id)
        val names = List(2) { "live-purchased-${UUID.randomUUID()}.db" }
        val dbs = names.map { Room.databaseBuilder(context, AppDatabase::class.java, it).build() }
        val sync = dbs.map { SyncController(it, remote) }
        val repo = dbs.map { RoomFamilyRepository(it) }
        val title = "Disposable purchased ${UUID.randomUUID()}"
        val cleanup = mutableListOf<Pair<String, String>>()
        try {
            sync.forEach { it.synchronize() }
            val list = repo[0].addList(title, 1002)
            val product = repo[0].addCatalogItem(title, "Other")
            sync[0].store.pending().forEach {
                cleanup.add(it.json.getValue("entity").jsonPrimitive.content to it.json.getValue("id").jsonPrimitive.content)
            }
            val old = repo[0].addCatalogItemToList(list, product)
            repo[0].setQty(old, 5); repo[0].toggleChecked(old, true)
            sync[0].synchronize(); sync[1].synchronize()
            val secondList = repo[1].getLists().single { it.name == title }.id
            val secondProduct = withTimeout(5_000) { repo[1].observeCatalogItems().first() }.single { it.title == title }.id
            assertTrue(repo[1].getItems(secondList, 0).single().checked)
            repo[1].addCatalogItemToList(secondList, secondProduct, 2)
            sync[1].synchronize(); sync[0].synchronize()
            val next = repo[0].getItems(list, 0).single()
            assertNotEquals(old, next.id); assertEquals(2, next.qty); assertFalse(next.checked)
            repo[1].toggleChecked(repo[1].getItems(secondList, 0).single().id, true)
            repo[1].clearPurchased(secondList)
            sync[1].synchronize(); sync[0].synchronize()
            assertTrue(repo[0].getItems(list, 0).isEmpty())
            assertTrue(repo[1].getItems(secondList, 0).isEmpty())
            assertTrue(withTimeout(5_000) { repo[0].observeCatalogItems().first() }.any { it.id == product })
            assertEquals(0, sync[0].store.pendingCount()); assertEquals(0, sync[1].store.pendingCount())
        } finally {
            try {
                remote.apply(buildJsonArray { cleanup.forEach { (entity, id) -> add(buildJsonObject {
                    put("operation_id", UUID.randomUUID().toString()); put("entity", entity); put("id", id)
                    put("action", "delete"); put("body", buildJsonObject {})
                }) } })
            } finally { dbs.forEach { it.close() }; names.forEach { context.deleteDatabase(it) } }
        }
    }

    @Test fun mergedListsKeepEveryEntryAndRedirectQueuedOldDeviceWrites() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("syncLive") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accounts = AccountRepository.getInstance(context)
        val session = withTimeout(30_000) {
            requireNotNull(accounts.status).filterIsInstance<SessionStatus.Authenticated>().first()
        }
        val remote = SupabaseSyncTransport(accounts, requireNotNull(session.session.user).id)
        val names = List(2) { "live-merge-test-${UUID.randomUUID()}.db" }
        val dbs = names.map { Room.databaseBuilder(context, AppDatabase::class.java, it).build() }
        val controllers = dbs.map { SyncController(it, remote) }
        val repos = dbs.map { RoomFamilyRepository(it) }
        val title = "Disposable merge verification ${UUID.randomUUID()}"
        val cleanup = mutableListOf<String>()
        try {
            controllers.forEach { it.synchronize() }
            val source = repos[0].addList(title, 1001)
            val target = repos[0].addList(title, 1002)
            val pending = controllers[0].store.pending().map { it.json }
            cleanup.addAll(pending.map { it.getValue("id").jsonPrimitive.content })
            val checked = repos[0].addItem(source, "First disposable product", 0)
            repos[0].setQty(checked, 4); repos[0].toggleChecked(checked, true)
            repos[0].addItem(target, "Second disposable product", 0)
            controllers[0].synchronize(); controllers[1].synchronize()
            val sourceB = repos[1].getLists().single { it.name == title && it.sortOrder == 1001 }.id
            repos[1].addItem(sourceB, "Queued offline disposable product", 0)
            remote.mergeLists(cleanup[0], cleanup[1])
            controllers[1].synchronize(); controllers[0].synchronize()
            for (index in 0..1) {
                val list = repos[index].getLists().single { it.name == title }
                val items = repos[index].getItems(list.id, 0)
                assertEquals(3, items.size)
                assertTrue(items.single { it.title == "First disposable product" }.checked)
                assertEquals(4, items.single { it.title == "First disposable product" }.qty)
                assertFalse(items.single { it.title == "Queued offline disposable product" }.checked)
            }
        } finally {
            try {
                cleanup.forEach { id -> remote.apply(buildJsonArray { add(buildJsonObject {
                    put("operation_id", UUID.randomUUID().toString()); put("entity", "list")
                    put("id", id); put("action", "delete"); put("body", buildJsonObject {})
                }) }) }
            } finally { dbs.forEach { it.close() }; names.forEach { context.deleteDatabase(it) } }
        }
    }

    @Test fun twoClientsConvergeAgainstRealService() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("syncLive") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accounts = AccountRepository.getInstance(context)
        val session = withTimeout(30_000) {
            requireNotNull(accounts.status).filterIsInstance<SessionStatus.Authenticated>().first()
        }
        val remote = SupabaseSyncTransport(accounts, requireNotNull(session.session.user).id)
        val names = List(2) { "live-sync-test-${UUID.randomUUID()}.db" }
        val dbs = names.map { Room.databaseBuilder(context, AppDatabase::class.java, it).build() }
        val a = SyncController(dbs[0], remote)
        val b = SyncController(dbs[1], remote)
        val repoA = RoomFamilyRepository(dbs[0])
        val repoB = RoomFamilyRepository(dbs[1])
        val title = "Disposable sync verification ${UUID.randomUUID()}"
        var remoteListId: String? = null
        try {
            a.synchronize(); b.synchronize()
            val listA = repoA.addList(title, 1000)
            val insert = a.store.pending().single()
            remoteListId = insert.json.getValue("id").jsonPrimitive.content
            a.synchronize()
            // Replaying an already acknowledged request must not duplicate its effects.
            remote.apply(JsonArray(listOf(insert.json)))
            b.synchronize()
            val listB = repoB.getLists().single { it.name == title }.id
            repoA.addItem(listA, "Disposable test product", 0)
            a.synchronize(); b.synchronize()
            val itemA = repoA.getItems(listA, 0).single().id
            val itemB = repoB.getItems(listB, 0).single().id
            // Both clients edit before either downloads the other's change.
            repoA.changeQty(itemA, 1)
            repoB.changeQty(itemB, 1)
            repoB.toggleChecked(itemB, true)
            a.synchronize(); b.synchronize(); a.synchronize()
            assertEquals(3, repoA.getItems(listA, 0).single().qty)
            assertEquals(3, repoB.getItems(listB, 0).single().qty)
            assertTrue(repoA.getItems(listA, 0).single().checked)
            repoA.renameList(listA, "$title renamed")
            a.synchronize(); b.synchronize()
            assertEquals("$title renamed", repoB.getLists().single { it.id == listB }.name)
            repoB.deleteList(listB)
            b.synchronize(); a.synchronize()
            assertFalse(repoA.getLists().any { it.id == listA })
            assertEquals(0, a.store.pendingCount())
            assertEquals(0, b.store.pendingCount())
        } finally {
            try {
                remoteListId?.let { id ->
                    remote.apply(buildJsonArray { add(buildJsonObject {
                        put("operation_id", UUID.randomUUID().toString()); put("entity", "list")
                        put("id", id); put("action", "delete"); put("body", buildJsonObject {})
                    }) })
                }
            } finally {
                dbs.forEach { it.close() }
                names.forEach { context.deleteDatabase(it) }
            }
        }
    }
}
