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
        val knownPaths = snapshot.mapNotNull { it.jsonObject["photo_path"]?.jsonPrimitive?.contentOrNull }.toMutableSet()
        val released = mutableSetOf<String>()
        while (true) {
            val changes = store.pending()
            if (changes.isEmpty()) break
            var progressed = false
            for (change in changes) {
                val target = targets[change.remoteId]
                // A new item can be created while ordinary sync finishes. Defer until the
                // next cycle instead of discarding it; confirmed revocations archive in pending().
                if (target == null) continue
                var path = change.photoPath
                if (change.mask and 1 != 0 && path == null && change.photoFile != null) {
                    val photoStore = checkNotNull(photos) { "Photo store unavailable" }
                    val bytes = withContext(Dispatchers.IO) { photoStore.uploadBytes(change.photoFile) }
                    path = target.getValue("owner_id").jsonPrimitive.content + "/" + target.getValue("list_id").jsonPrimitive.content + "/" + ProductPhotoStore.digest(bytes) + ".jpg"
                    if (path !in knownPaths) { remote.uploadPhoto(path, bytes); knownPaths.add(path) }
                }
                remote.applyMedia(change, target, path)
                store.acknowledge(change, path)
                change.photoFile?.let(released::add)
                progressed = true
            }
            if (!progressed) break
        }
        snapshot = remote.mediaSnapshot()
        released.addAll(store.apply(snapshot))
        photos?.let { photoStore ->
            for ((id,path,name) in store.localPhotos()) {
                if (!withContext(Dispatchers.IO) { photoStore.file(name).isFile }) store.invalidateMissingFile(id,path,name)
            }
            for ((id,path) in store.missingPhotos()) {
                val hash = path.substringAfterLast('/').removeSuffix(".jpg")
                val existing = photoStore.cacheName(hash)
                val cached = withContext(Dispatchers.IO) {
                    photoStore.file(existing).takeIf { it.isFile && it.length() <= ProductPhotoStore.MAX_BYTES }
                        ?.readBytes()?.let { ProductPhotoStore.digest(it) == hash } ?: false
                }
                val name = if (cached) existing else {
                    val bytes = remote.downloadPhoto(path)
                    require(bytes.size <= ProductPhotoStore.MAX_BYTES && hash == ProductPhotoStore.digest(bytes))
                    withContext(Dispatchers.IO) { photoStore.cache(bytes) }
                }
                store.cachePhoto(id,path,name)
            }
            // Cached downloads disappear when the last authorized local entry is removed.
            for (name in released + withContext(Dispatchers.IO) { photoStore.cachedNames() }) {
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
