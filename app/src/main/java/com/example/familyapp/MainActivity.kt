package com.example.familyapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
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

class MainActivity : ComponentActivity() {
    private val accountViewModel: AccountViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accountViewModel.handleCallback(intent.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) accountViewModel.handleCallback(intent.data)
        setContent {
            FamilyAppTheme {
                val account by accountViewModel.state.collectAsState()
                var accountOpen by rememberSaveable { mutableStateOf(false) }
                val locked = account.loading || (account.requiresSignIn && account.userId == null)
                if (accountOpen || locked) {
                    AccountScreen(account, accountViewModel::signIn, accountViewModel::signOut,
                        onBack = if (locked) null else ({ accountOpen = false }))
                    BackHandler(enabled = !locked) { accountOpen = false }
                } else {
                    key(account.databaseName) {
                        val db = remember { AppDatabase.getInstance(applicationContext, account.databaseName) }
                        LaunchedEffect(db) {
                            val setup = if (account.databaseName == "familyapp.db") "familyapp_setup"
                                else "setup_${account.databaseName}"
                            DefaultShoppingLists.initialize(db, getSharedPreferences(setup, MODE_PRIVATE))
                        }
                        val owner = remember { object : ViewModelStoreOwner {
                            override val viewModelStore = ViewModelStore()
                        } }
                        DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                        val repo = remember(db) { RoomFamilyRepository(db, ProductPhotoStore(applicationContext)) }
                        val mainMenuViewModel: MainMenuViewModel = viewModel(
                            viewModelStoreOwner = owner,
                            factory = MainMenuViewModelFactory(repository = repo)
                        )
                        FamilyAppRoot(mainMenuViewModel,
                            accountLabel = account.email ?: "Account",
                            onAccount = { accountOpen = true })
                    }
                }
            }
        }
    }
}
