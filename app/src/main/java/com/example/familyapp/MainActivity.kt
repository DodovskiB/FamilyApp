package com.example.familyapp

import android.app.Application
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.ui.theme.FamilyAppTheme
import com.example.familyapp.ui.viewmodel.ChecklistViewModel
import com.example.familyapp.ui.viewmodel.ChecklistViewModelFactory
import com.example.familyapp.ui.viewmodel.MainMenuViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
fun MainMenuScreen(onOpenList: (ListEntity) -> Unit) {
    val vm: MainMenuViewModel = viewModel()

    val lists by vm.lists.collectAsState()
    val loading by vm.loading.collectAsState()

    var newListName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("FamilyApp", style = MaterialTheme.typography.headlineSmall)
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
                    vm.addList(newListName)
                    newListName = ""
                }
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            if (lists.isEmpty()) {
                Text("Нема листи. Додај прва листа погоре.")
            } else {
                Text("Твои листи", style = MaterialTheme.typography.titleMedium)
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
fun ChecklistScreen(list: ListEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val vm: ChecklistViewModel = viewModel(
        factory = ChecklistViewModelFactory(app, list.id)
    )

    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()

    var newItem by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Shopping, 1 = Standard

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(list.name, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Shopping list") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Shopping items") }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Add item (за сега и за двата таба)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (selectedTab == 0)
                            "Нов продукт за купување"
                        else
                            "Нов стандарден item"
                    )
                },
                singleLine = true
            )
            Button(
                onClick = {
                    vm.addItem(newItem)
                    newItem = ""
                }
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            if (items.isEmpty()) {
                Text("Нема items во оваа листа.")
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
                                    vm.toggleChecked(item, checked)
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
