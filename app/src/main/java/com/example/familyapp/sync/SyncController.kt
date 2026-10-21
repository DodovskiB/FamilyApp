package com.example.familyapp.sync

import com.example.familyapp.data.db.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

data class SyncStatus(val message: String = "Connecting…", val pending: Int = 0, val ready: Boolean = false,
                      val needsImportChoice: Boolean = false)

class SyncController(db: AppDatabase, private val remote: SyncTransport) {
    val store = SyncStore(db)
    private val mutableStatus = MutableStateFlow(SyncStatus())
    val status = mutableStatus.asStateFlow()

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
    suspend fun synchronize(initializeDefaults: suspend () -> Unit = {}) {
        store.prepare()
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
                        return
                    }
                    "merge" -> store.mergeInitialLists(matches)
                }
            }
            store.apply(initial)
            if (initial.isEmpty()) initializeDefaults()
            store.finishInitialDownload()
        }
        while (true) {
            currentCoroutineContext().ensureActive()
            val changes = store.pending()
            if (changes.isEmpty()) break
            remote.apply(JsonArray(changes.map { it.json }))
            store.acknowledge(changes.map { it.operationId })
        }
        store.apply(download(store.cursor()))
        val pending = store.pendingCount()
        mutableStatus.value = SyncStatus(if (pending == 0) "Up to date" else "Waiting to sync", pending, true)
    }

    /** Runs only while the app is foregrounded; persisted changes retry after reopening. */
    suspend fun run(initializeDefaults: suspend () -> Unit) {
        while (true) {
            try { synchronize(initializeDefaults) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                val pending = runCatching { store.pendingCount() }.getOrDefault(0)
                mutableStatus.value = SyncStatus("Waiting for connection — changes stay on this phone", pending)
            }
            delay(5_000)
        }
    }
}
