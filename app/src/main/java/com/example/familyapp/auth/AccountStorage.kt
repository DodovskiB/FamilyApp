package com.example.familyapp.auth

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/** Keeps the existing device database, but never lends it to a different account. */
class AccountStorage(context: Context) {
    private val preferences = context.getSharedPreferences("familyapp_accounts", Context.MODE_PRIVATE)

    val requiresSignIn: Boolean get() = preferences.contains("local_owner")

    fun databaseName(userId: String): String = synchronized(ownershipLock) {
        val id = UUID.fromString(userId).toString()
        var owner = preferences.getString("local_owner", null)
        if (owner == null) {
            check(preferences.edit().putString("local_owner", id).commit()) {
                "Could not associate local data with this account."
            }
            owner = id
        }
        if (owner == id) "familyapp.db" else databaseNameFor(id)
    }

    companion object {
        private val ownershipLock = Any()
        fun databaseNameFor(userId: String): String {
            val canonicalId = UUID.fromString(userId).toString()
            val digest = MessageDigest.getInstance("SHA-256").digest(canonicalId.toByteArray())
                .joinToString("") { "%02x".format(it) }
            return "familyapp_account_$digest.db"
        }
    }
}
