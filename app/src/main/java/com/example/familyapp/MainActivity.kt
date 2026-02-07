@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.familyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.repo.RoomFamilyRepository
import com.example.familyapp.ui.MainMenuScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.familyapp.ui.theme.FamilyAppTheme
import com.example.familyapp.viewmodel.MainMenuViewModel

private enum class Screen {
    MAIN_MENU,
    CHECKLIST
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyAppTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current

    // DB + Repo (Room)
    val db = remember { AppDatabase.get(context) }
    val repo = remember { RoomFamilyRepository(db.listDao(), db.itemDao()) }

    // ViewModel (со Factory за да може да прима repo)
    val mainMenuVm: MainMenuViewModel = viewModel(
        factory = MainMenuViewModelFactory(repo)
    )

    // IMPORTANT: enum Screen не го чуваме во rememberSaveable (правеше crash)
    var screen by remember { mutableStateOf(Screen.MAIN_MENU) }

    // избрана листа
    var selectedListId by rememberSaveable { mutableLongStateOf(-1L) }
    var selectedListName by rememberSaveable { mutableStateOf("") }

    // Load defaults (Дома, Викендица) ако нема листи
    LaunchedEffect(Unit) {
        mainMenuVm.ensureDefaults()
    }

    when (screen) {
        Screen.MAIN_MENU -> {
            val lists by mainMenuVm.lists.collectAsState()

            MainMenuScreen(
                lists = lists,
                onOpenList = { listId, listName ->
                    selectedListId = listId
                    selectedListName = listName
                    screen = Screen.CHECKLIST
                },
                onAddList = { name ->
                    mainMenuVm.addList(name)
                },
                onRenameList = { listId, newName ->
                    // ако уште не ти е спремно rename во repo, само избриши го ова повикување
                    mainMenuVm.renameList(listId, newName)
                },
                onDeleteList = { listId ->
                    // ако уште не ти е спремно delete во repo, само избриши го ова повикување
                    mainMenuVm.deleteList(listId)
                }
            )
        }

        Screen.CHECKLIST -> {
            ChecklistScreen(
                title = selectedListName,
                onBack = { screen = Screen.MAIN_MENU }
            )
        }
    }
}

@Composable
private fun ChecklistScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "Shopping List" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ова е placeholder за Checklist screen.", style = MaterialTheme.typography.titleMedium)
            Text("Следно: тука ќе ги врземе items од Room по listId.")
        }
    }
}

private class MainMenuViewModelFactory(
    private val repo: RoomFamilyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainMenuViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
