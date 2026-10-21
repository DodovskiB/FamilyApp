package com.example.familyapp.sync

import com.example.familyapp.BuildConfig
import com.example.familyapp.auth.AccountRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
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

class SupabaseSyncTransport(private val accounts: AccountRepository, private val userId: String) : SyncTransport, SharingTransport {
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

    companion object {
        private val http = HttpClient(Android) {
            install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
        }
    }
}

class ListAccessDenied : Exception("List access unavailable")
