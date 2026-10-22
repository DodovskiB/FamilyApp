package com.example.familyapp.sync

import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.photo.ProductPhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

interface ItemMediaTransport {
    suspend fun mediaSnapshot(): JsonArray
    suspend fun applyMedia(change: MediaChange, target: JsonObject, photoPath: String?)
    suspend fun uploadPhoto(path: String, bytes: ByteArray)
    suspend fun downloadPhoto(path: String): ByteArray
    suspend fun obsoletePhotos(): List<String>
    suspend fun deletePhoto(path: String)
}

class ItemMediaSynchronizer(db: AppDatabase, private val remote: ItemMediaTransport, private val photos: ProductPhotoStore?) {
    val store = ItemMediaStore(db)
    private var lastCleanup = 0L

    suspend fun synchronize() {
        var snapshot = remote.mediaSnapshot()
        store.seed(snapshot)
        val targets = snapshot.associate { it.jsonObject.getValue("id").jsonPrimitive.content to it.jsonObject }
        while (true) {
            val changes = store.pending()
            if (changes.isEmpty()) break
            for (change in changes) {
                val target = targets[change.remoteId]
                if (target == null) { store.acknowledge(change, change.photoPath); continue }
                var path = change.photoPath
                if (change.mask and 1 != 0 && path == null && change.photoFile != null) {
                    val photoStore = checkNotNull(photos) { "Photo store unavailable" }
                    val bytes = withContext(Dispatchers.IO) { photoStore.uploadBytes(change.photoFile) }
                    path = target.getValue("owner_id").jsonPrimitive.content + "/" + target.getValue("list_id").jsonPrimitive.content + "/" + ProductPhotoStore.digest(bytes) + ".jpg"
                    remote.uploadPhoto(path, bytes)
                }
                remote.applyMedia(change, target, path)
                store.acknowledge(change, path)
            }
        }
        snapshot = remote.mediaSnapshot()
        store.apply(snapshot)
        photos?.let { photoStore ->
            for ((id,path) in store.missingPhotos()) {
                val bytes = remote.downloadPhoto(path)
                require(path.substringAfterLast('/').removeSuffix(".jpg") == ProductPhotoStore.digest(bytes))
                val name = withContext(Dispatchers.IO) { photoStore.cache(bytes) }
                store.cachePhoto(id,path,name)
            }
            // Cached downloads disappear when the last authorized local entry is removed.
            for (name in withContext(Dispatchers.IO) { photoStore.cachedNames() }) {
                if (!store.referencedPhoto(name)) withContext(Dispatchers.IO) { photoStore.delete(name) }
            }
        }
        // A grace period on the server protects in-flight uploads; cleanup never blocks list sync.
        val now = System.currentTimeMillis()
        if (now - lastCleanup > 60 * 60 * 1000L) {
            lastCleanup = now
            try { for (path in remote.obsoletePhotos()) remote.deletePhoto(path) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Best-effort reclamation retries in the next session/hour. */ }
        }
    }
}
