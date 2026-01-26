package com.example.familyapp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familyapp.data.entity.ItemEntity
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
                    AppRouter(application = application)
                }
            }
        }
    }
}

@Composable
private fun AppRouter(application: Application) {
    var selectedListId by remember { mutableStateOf<Long?>(null) }
    var selectedListName by remember { mutableStateOf("") }

    if (selectedListId == null) {
        MainMenuScreen(
            onOpenList = { id, name ->
                selectedListId = id
                selectedListName = name
            }
        )
    } else {
        BackHandler { selectedListId = null }

        ChecklistScreen(
            application = application,
            listId = selectedListId!!,
            listName = selectedListName,
            onBack = { selectedListId = null }
        )
    }
}

@Composable
fun MainMenuScreen(
    vm: MainMenuViewModel = viewModel(),
    onOpenList: (Long, String) -> Unit
) {
    val lists by vm.lists.collectAsState()
    var newListName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Family Lists", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Row {
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("New list name") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                vm.addList(newListName)
                newListName = ""
            }) { Text("Add") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (lists.isEmpty()) {
            Text("No lists yet. Add one above.")
        } else {
            LazyColumn {
                items(lists) { list ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onOpenList(list.id, list.name) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(list.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistScreen(
    application: Application,
    listId: Long,
    listName: String,
    onBack: () -> Unit
) {
    val vm: ChecklistViewModel = viewModel(
        factory = ChecklistViewModelFactory(application, listId)
    )

    val items by vm.items.collectAsState()

    var newItem by remember { mutableStateOf("") }
    var tabIndex by remember { mutableStateOf(0) } // 0=Shopping list, 1=Shopping items

    LaunchedEffect(tabIndex) { vm.setTab(tabIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(listName, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("Shopping list") }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("Shopping items") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add item") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                vm.addItem(newItem)
                newItem = ""
            }) { Text("Add") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Text("No items yet.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { item ->
                    ItemRow(
                        item = item,
                        showDelete = (tabIndex == 1),
                        onToggle = { checked -> vm.toggleChecked(item, checked) },
                        onDelete = { vm.deleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: ItemEntity,
    showDelete: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(item.title, style = MaterialTheme.typography.bodyLarge)

        Row {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onToggle
            )

            if (showDelete) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
