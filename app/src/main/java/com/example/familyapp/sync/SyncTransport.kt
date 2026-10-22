package com.example.familyapp.sync

import com.example.familyapp.BuildConfig
import com.example.familyapp.auth.AccountRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import kotlinx.serialization.json.*

interface SyncTransport {
    suspend fun apply(operations: JsonArray)
    suspend fun pull(afterRevision: Long): JsonArray
}

interface SharingTransport {
    suspend fun snapshot(): JsonObject
    suspend fun applyShared(owner: String, list: String, operations: JsonArray)
    suspend fun invite(list: String, email: String)
    suspend fun respond(invitation: String, accept: Boolean)
    suspend fun remove(list: String, member: String? = null, invitation: String? = null)
}

class SupabaseSyncTransport(private val accounts: AccountRepository, private val userId: String) : SyncTransport, SharingTransport, ItemMediaTransport {
    // A request keeps the token belonging to the expected account, even if the UI signs out
    // or changes accounts while the request is in flight. Credentials are never logged.
    private suspend fun rpc(name: String, parameters: JsonObject): String {
        val token = accounts.accessTokenFor(userId)
        val response = http.post("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/$name") {
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(parameters.toString())
        }
        if (response.status.value == 403) throw ListAccessDenied()
        check(response.status.isSuccess()) { "Sync service returned HTTP ${response.status.value}" }
        return response.bodyAsText()
    }

    override suspend fun apply(operations: JsonArray) {
        rpc("shopping_sync_apply", buildJsonObject { put("operations", operations) })
    }

    override suspend fun pull(afterRevision: Long): JsonArray =
        Json.parseToJsonElement(rpc("shopping_sync_pull", buildJsonObject { put("after_revision", afterRevision) })).jsonArray

    suspend fun mergeLists(sourceId: String, targetId: String) {
        rpc("shopping_sync_merge_lists", buildJsonObject { put("source_id", sourceId); put("target_id", targetId) })
    }

    override suspend fun snapshot() = Json.parseToJsonElement(rpc("shopping_sharing_snapshot", buildJsonObject {})).jsonObject
    override suspend fun applyShared(owner: String, list: String, operations: JsonArray) {
        rpc("shopping_sharing_apply", buildJsonObject { put("owner_id", owner); put("list_id", list); put("operations", operations) })
    }
    override suspend fun invite(list: String, email: String) {
        rpc("shopping_sharing_invite", buildJsonObject { put("list_id", list); put("email", email) })
    }
    override suspend fun respond(invitation: String, accept: Boolean) {
        rpc("shopping_sharing_respond", buildJsonObject { put("invitation_id", invitation); put("accept", accept) })
    }
    override suspend fun remove(list: String, member: String?, invitation: String?) {
        rpc("shopping_sharing_remove", buildJsonObject { put("list_id", list); put("member_id", member); put("invitation_id", invitation) })
    }

    override suspend fun mediaSnapshot(): JsonArray = Json.parseToJsonElement(rpc("shopping_media_snapshot", buildJsonObject {})).jsonArray

    override suspend fun applyMedia(change: MediaChange, target: JsonObject, photoPath: String?) {
        rpc("shopping_media_apply", buildJsonObject {
            put("owner_id", target.getValue("owner_id")); put("list_id", target.getValue("list_id"))
            put("item_id", change.remoteId); put("operation_id", change.operationId)
            put("field_mask", change.mask); put("link_url", change.linkUrl?.let(::JsonPrimitive) ?: JsonNull)
            put("photo_path", photoPath?.let(::JsonPrimitive) ?: JsonNull)
        })
    }

    private fun validatePhotoPath(path: String) {
        require(path.matches(Regex("[a-f0-9-]{36}/[a-f0-9-]{36}/[a-f0-9]{64}\\.jpg")))
    }

    override suspend fun uploadPhoto(path: String, bytes: ByteArray) {
        validatePhotoPath(path)
        require(bytes.size in 1..com.example.familyapp.data.photo.ProductPhotoStore.MAX_BYTES)
        val token = accounts.accessTokenFor(userId)
        val response = http.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/shopping-photos/$path") {
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Image.JPEG); setBody(bytes)
        }
        // Immutable, content-addressed object. A retry of the same bytes may already exist.
        check(response.status.isSuccess() || response.status.value == 409 ||
            (response.status.value == 400 && response.bodyAsText().let { it.contains("Duplicate") || it.contains("already exists") })) { "Photo upload failed" }
    }

    override suspend fun downloadPhoto(path: String): ByteArray {
        validatePhotoPath(path)
        val token = accounts.accessTokenFor(userId)
        val response = http.get("${BuildConfig.SUPABASE_URL}/storage/v1/object/authenticated/shopping-photos/$path") {
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        check(response.status.isSuccess()) { "Photo download failed" }
        val bytes = response.bodyAsBytes()
        require(bytes.size in 1..com.example.familyapp.data.photo.ProductPhotoStore.MAX_BYTES)
        return bytes
    }

    override suspend fun obsoletePhotos(): List<String> = Json.parseToJsonElement(rpc("shopping_media_orphans", buildJsonObject {}))
        .jsonArray.map { it.jsonPrimitive.content }

    override suspend fun deletePhoto(path: String) {
        validatePhotoPath(path)
        val token = accounts.accessTokenFor(userId)
        val response = http.delete("${BuildConfig.SUPABASE_URL}/storage/v1/object/shopping-photos") {
            header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("prefixes", buildJsonArray { add(path) }) }.toString())
        }
        check(response.status.isSuccess()) { "Photo cleanup failed" }
    }

    companion object {
        private val http = HttpClient(Android) {
            install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
        }
    }
}

class ListAccessDenied : Exception("List access unavailable")
