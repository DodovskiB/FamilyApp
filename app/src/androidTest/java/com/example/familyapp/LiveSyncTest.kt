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
