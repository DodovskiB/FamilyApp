package com.example.familyapp.auth

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class AccountState(
    val configured: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val databaseName: String = "familyapp.db",
    val requiresSignIn: Boolean = false,
    val error: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AccountRepository.getInstance(application)
    private val storage = AccountStorage(application)
    private val mutableState = MutableStateFlow(AccountState(
        configured = repository.configured, loading = repository.configured,
        requiresSignIn = storage.requiresSignIn
    ))
    val state: StateFlow<AccountState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.status?.collectLatest { status ->
                when (status) {
                    SessionStatus.Initializing -> mutableState.update { it.copy(loading = true) }
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user == null) {
                            mutableState.value = AccountState(configured = true, loading = false,
                                requiresSignIn = true, error = "Please sign in again.")
                        } else {
                            // Hide the previous account immediately while resolving storage.
                            mutableState.update { it.copy(loading = true, userId = null) }
                            try {
                                val database = withContext(Dispatchers.IO) { storage.databaseName(user.id) }
                                mutableState.value = AccountState(configured = true, loading = false,
                                    userId = user.id, email = user.email,
                                    name = user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull,
                                    databaseName = database, requiresSignIn = true)
                            } catch (error: CancellationException) { throw error }
                            catch (_: Exception) {
                                mutableState.value = AccountState(configured = true, loading = false,
                                    requiresSignIn = true, error = "Could not open account data. Please try again.")
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated, is SessionStatus.RefreshFailure ->
                        mutableState.value = AccountState(configured = true, loading = false,
                            requiresSignIn = storage.requiresSignIn,
                            error = if (status is SessionStatus.RefreshFailure)
                                "Could not restore your session. Connect to the internet and sign in again." else null)
                }
            }
        }
    }

    fun signIn() = runAction("Could not start Google sign-in. Please check your connection and try again.") {
        repository.signIn()
    }

    fun handleCallback(uri: Uri?) {
        if (uri == null || !repository.configured || !AccountRepository.isCallback(uri)) return
        runAction("Sign-in could not be completed. Please try again.") { repository.finishSignIn(uri) }
    }

    fun signOut() = runAction("Signed out on this device. The server could not be reached.") {
        repository.signOut()
    }

    private fun runAction(message: String, action: suspend () -> Unit) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null) }
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(error = message) } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }
}
