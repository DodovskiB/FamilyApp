package com.example.familyapp.sync

import com.example.familyapp.data.db.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

data class SyncStatus(val message: String = "Connecting…", val pending: Int = 0, val ready: Boolean = false,
                      val needsImportChoice: Boolean = false)

class SyncController(db: AppDatabase, private val remote: SyncTransport,
                     photos: com.example.familyapp.data.photo.ProductPhotoStore? = null) {
    private val media = (remote as? ItemMediaTransport)?.let { ItemMediaSynchronizer(db, it, photos) }
    val store = SyncStore(db)
    private val mutableStatus = MutableStateFlow(SyncStatus())
    val status = mutableStatus.asStateFlow()
    private val mutex = Mutex()
    private val mutableSharing = MutableStateFlow(SharingState())
    val sharing = mutableSharing.asStateFlow()

    private suspend fun refreshSharing() {
        val transport = remote as? SharingTransport ?: return
        val snapshot = transport.snapshot()
        val access = snapshot.getValue("access").jsonArray.map { row ->
            val r = row.jsonObject
            ListAccess(r.getValue("id").jsonPrimitive.content, r.getValue("owner_id").jsonPrimitive.content, r.getValue("role").jsonPrimitive.content)
        }
        val removed = store.reconcileSharedAccess(access.filter { it.role == "admin" }.associate { it.id to it.owner })
        store.apply(snapshot.getValue("records").jsonArray, advanceCursor = false)
        val mappings = store.listMappings()
        mutableSharing.value = mutableSharing.value.copy(
            access = mappings.mapNotNull { (local, id) -> access.firstOrNull { it.id == id }?.let { local to it } }.toMap(),
            invitations = snapshot.getValue("invitations").jsonArray.map { row -> row.jsonObject.let {
                ListInvitation(it.getValue("id").jsonPrimitive.content, it.getValue("list_id").jsonPrimitive.content,
                    it.getValue("name").jsonPrimitive.content, it.getValue("email").jsonPrimitive.content, it.getValue("incoming").jsonPrimitive.boolean, it["group_id"]?.jsonPrimitive?.contentOrNull)
            } },
            members = snapshot.getValue("members").jsonArray.map { row -> row.jsonObject.let {
                ListMember(it.getValue("id").jsonPrimitive.content, it.getValue("list_id").jsonPrimitive.content, it.getValue("email").jsonPrimitive.content)
            } },
            message = if (removed > 0) "Access to a shared list ended. Its offline changes were kept privately and were not uploaded." else mutableSharing.value.message
        )
    }

    suspend fun restoreSharingAccess() {
        store.prepare()
        val shared = store.sharedAccess()
        mutableSharing.value = mutableSharing.value.copy(access = store.listMappings().mapNotNull { (local, remote) ->
            shared[remote]?.let { local to ListAccess(remote, it, "admin") }
        }.toMap(), ready = true)
    }

    suspend fun sharingAction(action: String, list: Long? = null, email: String = "", invitation: String? = null,
                              member: String? = null) = mutex.withLock {
        val transport = remote as? SharingTransport ?: return@withLock
        mutableSharing.value = mutableSharing.value.copy(busy = true, message = null)
        try {
            val remoteList = list?.let { store.remoteListId(it) }
            when (action) {
                "invite" -> transport.invite(requireNotNull(remoteList), email.trim())
                "accept_many", "decline_many" -> (remote as BatchSharingTransport).respondMany(requireNotNull(invitation), action == "accept_many")
                "accept", "decline" -> transport.respond(requireNotNull(invitation), action == "accept")
                "remove" -> transport.remove(requireNotNull(remoteList), member, invitation)
                "refresh" -> Unit
                else -> error("Unknown sharing action")
            }
            refreshSharing()
            if (action == "invite") mutableSharing.value = mutableSharing.value.copy(message = "Invitation added. They can accept it in Invitations using this Google email. No email was sent.")
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) {
            mutableSharing.value = mutableSharing.value.copy(message = "Could not complete this action. Check your connection, email and list access, then try again.")
        } finally { mutableSharing.value = mutableSharing.value.copy(busy = false) }
    }

    suspend fun inviteLists(lists: List<Long>, email: String) = mutex.withLock {
        mutableSharing.value = mutableSharing.value.copy(busy = true, message = null)
        try {
            require(lists.isNotEmpty())
            val ids = lists.distinct().map { local ->
                requireNotNull(mutableSharing.value.access[local]).also { require(it.role == "owner") }.id
            }
            (remote as BatchSharingTransport).inviteMany(ids, email.trim())
            refreshSharing()
            mutableSharing.value = mutableSharing.value.copy(message = "Invitation added. They can accept it in Invitations using this Google email. No email was sent.")
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) {
            mutableSharing.value = mutableSharing.value.copy(message = "Could not complete this action. Check your connection, email and list access, then try again.")
        } finally { mutableSharing.value = mutableSharing.value.copy(busy = false) }
    }

    suspend fun chooseImport(merge: Boolean) { store.chooseImport(merge) }

    private suspend fun download(after: Long): JsonArray {
        val records = linkedMapOf<String, JsonElement>()
        var cursor = after
        do {
            currentCoroutineContext().ensureActive()
            val page = remote.pull(cursor)
            page.forEach { row ->
                val data = row.jsonObject
                records[data.getValue("entity").jsonPrimitive.content + data.getValue("id").jsonPrimitive.content] = row
                cursor = maxOf(cursor, data.getValue("revision").jsonPrimitive.long)
            }
        } while (page.size == 500)
        return JsonArray(records.values.toList())
    }

    /** One cycle is separately testable with isolated databases and transports. */
    suspend fun synchronize(initializeDefaults: suspend () -> Unit = {}) = mutex.withLock {
        store.prepare()
        media?.store?.prepare()
        // Start durable capture before any network request; user edits during the first
        // download must be queued too, including deletions of freshly downloaded rows.
        store.seed()
        if (!store.hasInitialDownload()) {
            val initial = download(0)
            if (initial.isNotEmpty()) store.discardPristineDefaults()
            val matches = store.matchingInitialLists(initial)
            if (matches.isNotEmpty()) {
                when (store.importChoice()) {
                    "pending" -> {
                        mutableStatus.value = SyncStatus("This phone and your account have matching list names. Open Account to choose how to connect them.",
                            store.pendingCount(), needsImportChoice = true)
                        return@withLock
                    }
                    "merge" -> store.mergeInitialLists(matches)
                }
            }
            store.apply(initial)
            if (initial.isEmpty()) initializeDefaults()
            store.finishInitialDownload()
        }
        // Fetch revocations before touching the durable queue. Failed network reads keep it unchanged.
        refreshSharing()
        while (true) {
            currentCoroutineContext().ensureActive()
            val changes = store.pending()
            if (changes.isEmpty()) break
            // Preserve ordering; acknowledge each group only after its own server transaction succeeds.
            val firstList = store.sharedListFor(changes.first())
            val batch = mutableListOf<QueuedChange>()
            for (change in changes) {
                if (store.sharedListFor(change) != firstList) break
                batch.add(change)
            }
            try {
                if (firstList == null) remote.apply(JsonArray(batch.map { it.json }))
                else (remote as SharingTransport).applyShared(store.sharedAccess().getValue(firstList), firstList, JsonArray(batch.map { it.json }))
                store.acknowledge(batch.map { it.operationId })
            } catch (error: ListAccessDenied) {
                refreshSharing() // Revocation can race with the prior snapshot. Never fall back to owner upload.
                throw error
            }
        }
        store.apply(download(store.cursor()))
        refreshSharing()
        media?.synchronize()
        val pending = store.pendingCount() + (media?.store?.count() ?: 0)
        mutableStatus.value = SyncStatus(if (pending == 0) "Up to date" else "Waiting to sync", pending, true)
    }

    /** Runs only while the app is foregrounded; persisted changes retry after reopening. */
    suspend fun run(initializeDefaults: suspend () -> Unit) {
        restoreSharingAccess()
        while (true) {
            try { synchronize(initializeDefaults) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                val pending = runCatching { store.pendingCount() + (media?.store?.count() ?: 0) }.getOrDefault(0)
                mutableStatus.value = SyncStatus("Waiting for connection — changes stay on this phone", pending)
            }
            delay(5_000)
        }
    }
}
