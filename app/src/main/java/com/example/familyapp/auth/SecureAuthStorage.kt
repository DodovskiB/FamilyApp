package com.example.familyapp.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Tokens and PKCE verifiers are encrypted with a device key and excluded from backups. */
class SecureAuthStorage(context: Context) : SessionManager, CodeVerifierCache {
    private val directory = File(context.noBackupFilesDir, "authentication").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private suspend fun write(name: String, value: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            cipher.updateAAD(name.toByteArray())
            val bytes = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val file = AtomicFile(File(directory, name))
            val output = file.startWrite()
            try {
                output.write(bytes)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
        }
    }

    private suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val file = AtomicFile(File(directory, name))
            if (!file.baseFile.exists() && !File(directory, "$name.bak").exists()) return@synchronized null
            try {
                val bytes = file.readFully()
                require(bytes.size >= 28)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                cipher.updateAAD(name.toByteArray())
                cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
            } catch (_: Exception) {
                file.delete()
                null // Invalidated keys or damaged storage require a fresh sign-in.
            }
        }
    }

    private suspend fun remove(name: String) = withContext(Dispatchers.IO) {
        synchronized(lock) { AtomicFile(File(directory, name)).delete() }
    }

    override suspend fun saveSession(session: UserSession) =
        write("session", json.encodeToString(UserSession.serializer(), session))

    override suspend fun loadSession(): UserSession? {
        val value = read("session") ?: return null
        return try { json.decodeFromString(UserSession.serializer(), value) }
        catch (_: Exception) { deleteSession(); null }
    }

    override suspend fun deleteSession() = remove("session")
    override suspend fun saveCodeVerifier(codeVerifier: String) = write("verifier", codeVerifier)
    override suspend fun loadCodeVerifier(): String? = read("verifier")
    override suspend fun deleteCodeVerifier() = remove("verifier")

    companion object { private const val KEY_ALIAS = "familyapp_auth_v1" }
}
