package com.example.familyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.ui.theme.FamilyAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.HorizontalDivider


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (опционално) DB_TEST можеш да го оставиш или да го тргнеш подоцна
        val db = AppDatabase.get(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existing = db.listDao().getAll()
                if (existing.isEmpty()) {
                    db.listDao().insert(ListEntity(name = "Дома"))
                    db.listDao().insert(ListEntity(name = "Викендица"))
                }
            }
        }

        setContent {
            FamilyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FamilyAppRoot()
                }
            }
        }
    }
}

private sealed class Screen {
    data object MainMenu : Screen()
    data class Checklist(val list: ListEntity) : Screen()
}

@Composable
fun FamilyAppRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.MainMenu) }

    when (val s = screen) {
        Screen.MainMenu -> MainMenuScreen(
            onOpenList = { selected -> screen = Screen.Checklist(selected) }
        )
        is Screen.Checklist -> ChecklistScreen(
            list = s.list,
            onBack = { screen = Screen.MainMenu }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onOpenList: (ListEntity) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    var lists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var newListName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadLists() {
        scope.launch {
            isLoading = true
            val loaded = withContext(Dispatchers.IO) { db.listDao().getAll() }
            lists = loaded
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadLists() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "FamilyApp",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = newListName,
                onValueChange = { newListName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Нова листа (пример: Аптека)") },
                singleLine = true
            )
            Button(
                onClick = {
                    val name = newListName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.listDao().insert(ListEntity(name = name))
                            }
                            newListName = ""
                            loadLists()
                        }
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            if (lists.isEmpty()) {
                Text("Нема листи. Додај прва листа погоре.")
            } else {
                Text(
                    text = "Твои листи",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(lists) { list ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onOpenList(list) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(list.name, style = MaterialTheme.typography.bodyLarge)
                                Text("▶", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    list: ListEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<ItemEntity>>(emptyList()) }
    var newItem by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    fun loadItems() {
        scope.launch {
            isLoading = true
            val loaded = withContext(Dispatchers.IO) { db.itemDao().getByList(list.id) }
            items = loaded
            isLoading = false
        }
    }

    LaunchedEffect(list.id) { loadItems() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = list.name,
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Внеси нов продукт") },
                singleLine = true
            )
            Button(
                onClick = {
                    val title = newItem.trim()
                    if (title.isNotEmpty()) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.itemDao().insert(
                                    ItemEntity(
                                        listId = list.id,
                                        title = title,
                                        isChecked = false
                                    )
                                )
                            }
                            newItem = ""
                            loadItems()
                        }
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            if (items.isEmpty()) {
                Text("Нема продукти во оваа листа. Додај одозгора.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge)

                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.itemDao().update(item.copy(isChecked = checked))
                                        }
                                        // за да се рефрешне листата
                                        loadItems()
                                    }
                                }
                            )
                        }

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
