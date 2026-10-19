package com.example.familyapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel
import kotlinx.coroutines.launch

@Composable
fun FamilyAppRoot(viewModel: MainMenuViewModel) {
    var catalogOpen by rememberSaveable { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val screenState = rememberSaveableStateHolder()
    var newListOpen by rememberSaveable { mutableStateOf(false) }
    var newListName by rememberSaveable { mutableStateOf("") }
    val saving by viewModel.listSaving.collectAsState()
    val error by viewModel.listError.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                    Text("FamilyApp", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(24.dp))
                    NavigationDrawerItem(
                        label = { Text("Shopping lists") }, selected = !catalogOpen,
                        onClick = {
                            catalogOpen = false
                            scope.launch { drawer.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Item list") }, selected = catalogOpen,
                        onClick = {
                            catalogOpen = true
                            scope.launch { drawer.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("New list") }, selected = false,
                        onClick = {
                            viewModel.clearListError()
                            newListName = ""
                            newListOpen = true
                            scope.launch { drawer.close() }
                        }
                    )
                }
            }
        }
    ) {
        screenState.SaveableStateProvider(if (catalogOpen) "catalog" else "lists") {
            if (catalogOpen) {
                ShoppingItemsScreen(viewModel, onBack = { catalogOpen = false })
            } else {
                ShoppingListScreen(viewModel, onOpenMenu = { scope.launch { drawer.open() } })
            }
        }
    }
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    if (newListOpen) {
        AlertDialog(onDismissRequest = { if (!saving) newListOpen = false },
            title = { Text("Create list") },
            text = {
                Column {
                    OutlinedTextField(value = newListName, onValueChange = { newListName = it },
                        label = { Text("List name") }, singleLine = true, enabled = !saving)
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = newListName.isNotBlank() && !saving, onClick = {
                    viewModel.addList(newListName) {
                        newListOpen = false
                        catalogOpen = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { newListOpen = false }) { Text("Cancel") }
            })
    }
}
