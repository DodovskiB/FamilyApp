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
import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel
import kotlinx.coroutines.launch
import com.example.familyapp.sync.SharingState

@Composable
fun FamilyAppRoot(
    viewModel: MainMenuViewModel,
    accountLabel: String = "Account",
    onAccount: (() -> Unit)? = null,
    sharing: SharingState? = null,
    onSharingAction: (String, Long?, String, String?, String?) -> Unit = { _, _, _, _, _ -> }
) {
    var catalogOpen by rememberSaveable { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val screenState = rememberSaveableStateHolder()
    var newListOpen by rememberSaveable { mutableStateOf(false) }
    var newListName by rememberSaveable { mutableStateOf("") }
    val saving by viewModel.listSaving.collectAsState()
    val error by viewModel.listError.collectAsState()
    var sharingOpen by rememberSaveable { mutableStateOf(false) }
    var sharingList by rememberSaveable { mutableStateOf<Long?>(null) }
    if (sharingOpen && sharing != null) {
        SharingScreen(sharing, sharingList, onBack = { sharingOpen = false }, onAction = onSharingAction)
        return
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(24.dp))
                    if (onAccount != null) {
                        NavigationDrawerItem(
                            label = { Text(accountLabel) }, selected = false,
                            onClick = { scope.launch { drawer.close() }; onAccount() }
                        )
                    }
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
                    if (sharing != null) NavigationDrawerItem(
                        label = { Text("Invitations (${sharing.invitations.count { it.incoming }})") }, selected = false,
                        onClick = { sharingList = null; sharingOpen = true; scope.launch { drawer.close() } }
                    )
                }
            }
        }
    ) {
        screenState.SaveableStateProvider(if (catalogOpen) "catalog" else "lists") {
            if (catalogOpen) {
                ShoppingItemsScreen(viewModel, onBack = { catalogOpen = false })
            } else {
                ShoppingListScreen(viewModel, onOpenMenu = { scope.launch { drawer.open() } }, sharing = sharing,
                    onShare = { sharingList = it; sharingOpen = true })
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
