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

class SupabaseSyncTransport(private val accounts: AccountRepository, private val userId: String) : SyncTransport {
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
        check(response.status.isSuccess()) { "Sync service returned HTTP ${response.status.value}" }
        return response.bodyAsText()
    }

    override suspend fun apply(operations: JsonArray) {
        rpc("shopping_sync_apply", buildJsonObject { put("operations", operations) })
    }

    override suspend fun pull(afterRevision: Long): JsonArray =
        Json.parseToJsonElement(rpc("shopping_sync_pull", buildJsonObject { put("after_revision", afterRevision) })).jsonArray

    companion object {
        private val http = HttpClient(Android) {
            install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
        }
    }
}
