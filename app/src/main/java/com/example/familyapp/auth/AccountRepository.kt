package com.example.familyapp.auth

import android.content.Context
import android.net.Uri
import com.example.familyapp.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel

class AccountRepository private constructor(context: Context) {
    private val secureStorage = SecureAuthStorage(context.applicationContext)
    val configured = BuildConfig.SUPABASE_URL.startsWith("https://") &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    // Only a publishable/legacy anon key belongs in the APK. Never a service_role/secret key.
    private val client = if (configured) createSupabaseClient(
        BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        defaultLogLevel = LogLevel.NONE
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "com.example.familyapp"
            host = "auth-callback"
            sessionManager = secureStorage
            codeVerifierCache = secureStorage
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
    } else null

    val status get() = client?.auth?.sessionStatus

    suspend fun signIn() {
        val auth = requireNotNull(client).auth
        auth.awaitInitialization()
        auth.signInWith(Google)
    }

    suspend fun finishSignIn(uri: Uri) {
        require(isCallback(uri))
        require(uri.getQueryParameter("error") == null) { "Sign-in was cancelled or declined." }
        val code = requireNotNull(uri.getQueryParameter("code")) { "Missing sign-in code." }
        require(code.isNotBlank() && code.length < 4096)
        val auth = requireNotNull(client).auth
        auth.awaitInitialization()
        // The SDK validates possession of the locally stored PKCE verifier.
        auth.exchangeCodeForSession(code)
        secureStorage.deleteCodeVerifier()
    }

    suspend fun signOut() {
        val auth = requireNotNull(client).auth
        try {
            auth.signOut()
        } finally {
            // Lock local data even when the device is offline and remote revocation fails.
            auth.clearSession()
            secureStorage.deleteCodeVerifier()
        }
    }

    companion object {
        @Volatile private var instance: AccountRepository? = null
        fun getInstance(context: Context): AccountRepository = instance ?: synchronized(this) {
            instance ?: AccountRepository(context.applicationContext).also { instance = it }
        }

        fun isCallback(uri: Uri): Boolean = uri.scheme == "com.example.familyapp" &&
            uri.host == "auth-callback" && uri.path.orEmpty().isEmpty() && uri.fragment == null
    }
}
