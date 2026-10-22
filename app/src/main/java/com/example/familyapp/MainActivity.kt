package com.example.familyapp

import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import android.os.Bundle
import android.content.Intent
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.familyapp.auth.AccountRepository
import com.example.familyapp.sync.SyncController
import com.example.familyapp.sync.SupabaseSyncTransport
import com.example.familyapp.auth.AccountViewModel
import com.example.familyapp.auth.AccountScreen
import com.example.familyapp.data.DefaultShoppingLists
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.ui.FamilyAppRoot
import com.example.familyapp.ui.theme.FamilyAppTheme
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import repo.RoomFamilyRepository

class MainActivity : AppCompatActivity() {
    private val accountViewModel: AccountViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accountViewModel.handleCallback(intent.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Explicit English default, independent of the phone's language. AppCompat
        // restores a previous explicit choice before this point, also on Android 12.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        if (savedInstanceState == null) accountViewModel.handleCallback(intent.data)
        setContent {
            FamilyAppTheme {
                val account by accountViewModel.state.collectAsState()
                val db = remember(account.databaseName) { AppDatabase.getInstance(applicationContext, account.databaseName) }
                val setup = if (account.databaseName == "familyapp.db") "familyapp_setup" else "setup_${account.databaseName}"
                val sync = remember(account.userId, db) {
                    account.userId?.let { SyncController(db, SupabaseSyncTransport(AccountRepository.getInstance(applicationContext), it), ProductPhotoStore(applicationContext, account.databaseName)) }
                }
                val syncStatus = sync?.status?.collectAsState()?.value
                val sharing = sync?.sharing?.collectAsState()?.value
                val scope = rememberCoroutineScope()
                LaunchedEffect(sync) {
                    sync?.let { controller ->
                        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            controller.run { DefaultShoppingLists.initialize(db, getSharedPreferences(setup, MODE_PRIVATE)) }
                        }
                    }
                }
                var accountOpen by rememberSaveable { mutableStateOf(false) }
                val locked = account.loading || (account.requiresSignIn && account.userId == null)
                if (accountOpen || locked || syncStatus?.needsImportChoice == true) {
                    AccountScreen(account, accountViewModel::signIn, accountViewModel::signOut,
                        syncStatus = syncStatus,
                        onImportChoice = { merge -> scope.launch { sync?.chooseImport(merge) } },
                        onBack = if (locked) null else ({ accountOpen = false }))
                    BackHandler(enabled = !locked) { accountOpen = false }
                } else {
                    key(account.databaseName) {
                        LaunchedEffect(db) {
                            if (account.userId == null && !account.requiresSignIn) {
                                DefaultShoppingLists.initialize(db, getSharedPreferences(setup, MODE_PRIVATE))
                            }
                        }
                        val owner = remember { object : ViewModelStoreOwner {
                            override val viewModelStore = ViewModelStore()
                        } }
                        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                        val repo = remember(db) { RoomFamilyRepository(db, ProductPhotoStore(applicationContext, account.databaseName)) }
                        val mainMenuViewModel: MainMenuViewModel = viewModel(
                            viewModelStoreOwner = owner,
                            factory = MainMenuViewModelFactory(repository = repo)
                        )
                        FamilyAppRoot(mainMenuViewModel,
                            accountLabel = account.email ?: stringResource(R.string.account),
                            onAccount = { accountOpen = true }, sharing = sharing,
                            onSharingAction = { action, list, email, invitation, member ->
                                scope.launch { sync?.sharingAction(action, list, email, invitation, member) }
                            })
                    }
                }
            }
        }
    }
}
