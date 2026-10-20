package com.example.familyapp

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.auth.AccountRepository
import com.example.familyapp.auth.AccountStorage
import com.example.familyapp.auth.SecureAuthStorage
import io.github.jan.supabase.auth.user.UserSession
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountIsolationTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun existingDatabaseBelongsOnlyToFirstAccount() {
        val prefsName = "test_accounts_${UUID.randomUUID()}"
        val context = object : ContextWrapper(app) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
                app.getSharedPreferences(prefsName, mode)
        }
        val firstId = UUID.randomUUID().toString()
        val secondId = UUID.randomUUID().toString()
        try {
            val storage = AccountStorage(context)
            assertFalse(storage.requiresSignIn)
            assertEquals("familyapp.db", storage.databaseName(firstId))
            assertTrue(storage.requiresSignIn)
            val reopened = AccountStorage(context)
            assertNotEquals("familyapp.db", reopened.databaseName(secondId))
            assertEquals("familyapp.db", reopened.databaseName(firstId))
            assertEquals(AccountStorage.databaseNameFor(secondId), reopened.databaseName(secondId))
        } finally { app.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().commit() }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    @Test fun sessionAndVerifierAreEncryptedPersistedAndCleared() = runBlocking {
        val directory = File(app.cacheDir, "test_auth_${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            val storage = SecureAuthStorage(context)
            val session = UserSession(accessToken = "test-access-secret", refreshToken = "test-refresh-secret",
                expiresIn = 3600, tokenType = "bearer")
            storage.saveSession(session)
            storage.saveCodeVerifier("test-pkce-secret")
            assertFalse(File(directory, "authentication/session").readBytes()
                .toString(Charsets.UTF_8).contains("test-access-secret"))
            val reopened = SecureAuthStorage(context)
            assertEquals(session, reopened.loadSession())
            assertEquals("test-pkce-secret", reopened.loadCodeVerifier())
            File(directory, "authentication/session").writeBytes(byteArrayOf(1, 2, 3))
            assertNull(reopened.loadSession())
            reopened.deleteSession()
            reopened.deleteCodeVerifier()
            assertNull(reopened.loadCodeVerifier())
        } finally { directory.deleteRecursively() }
    }

    @Test fun callbackAcceptsOnlyExpectedPkceEndpoint() {
        fun uri(value: String) = android.net.Uri.parse(value)
        assertTrue(AccountRepository.isCallback(uri("com.example.familyapp://auth-callback?code=test")))
        assertFalse(AccountRepository.isCallback(uri("https://auth-callback?code=test")))
        assertFalse(AccountRepository.isCallback(uri("com.example.familyapp://other?code=test")))
        assertFalse(AccountRepository.isCallback(uri("com.example.familyapp://auth-callback#access_token=test")))
        assertFalse(AccountRepository.isCallback(uri("com.example.familyapp://auth-callback/other?code=test")))
    }
}
