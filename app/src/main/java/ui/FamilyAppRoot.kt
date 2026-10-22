package com.example.familyapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
    accountLabel: String = stringResource(R.string.account),
    onAccount: (() -> Unit)? = null,
    sharing: SharingState? = null,
    onSharingAction: (String, Long?, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onInviteLists: (List<Long>, String) -> Unit = { _, _ -> }
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
        val availableLists by viewModel.lists.collectAsState()
        SharingScreen(sharing, sharingList, onBack = { sharingOpen = false }, onAction = onSharingAction,
            ownedLists = availableLists, onInviteLists = onInviteLists)
        return
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxHeight().safeDrawingPadding().padding(16.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        if (onAccount != null) {
                            NavigationDrawerItem(
                                label = { Text(accountLabel, style = MaterialTheme.typography.titleMedium) }, selected = false,
                                onClick = { scope.launch { drawer.close() }; onAccount() }
                            )
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(20.dp))
                        }
                        if (sharing != null) NavigationDrawerItem(
                            label = { Text(stringResource(R.string.invitations_count, sharing.incomingCount)) }, selected = false,
                            onClick = { sharingList = null; sharingOpen = true; scope.launch { drawer.close() } }
                        )
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.shopping_lists)) }, selected = !catalogOpen,
                            onClick = {
                                catalogOpen = false
                                scope.launch { drawer.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.item_list)) }, selected = catalogOpen,
                            onClick = {
                                catalogOpen = true
                                scope.launch { drawer.close() }
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(stringResource(R.string.language), Modifier.weight(1f))
                        val language = LocalConfiguration.current.locales[0].language
                        listOf("en" to "EN", "mk" to "MK").forEach { (tag, label) ->
                            FilterChip(selected = language == tag, onClick = {
                                if (language != tag) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                            }, label = { Text(label) })
                        }
                    }
                }
            }
        }
    ) {
        screenState.SaveableStateProvider(if (catalogOpen) "catalog" else "lists") {
            if (catalogOpen) {
                ShoppingItemsScreen(viewModel, onBack = { catalogOpen = false })
            } else {
                ShoppingListScreen(viewModel, onOpenMenu = { scope.launch { drawer.open() } }, sharing = sharing,
                    onShare = { sharingList = it; sharingOpen = true },
                    onNewList = { viewModel.clearListError(); newListName = ""; newListOpen = true })
            }
        }
    }
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    if (newListOpen) {
        AlertDialog(onDismissRequest = { if (!saving) newListOpen = false },
            title = { Text(stringResource(R.string.create_list)) },
            text = {
                Column {
                    OutlinedTextField(value = newListName, onValueChange = { newListName = it },
                        label = { Text(stringResource(R.string.list_name)) }, singleLine = true, enabled = !saving)
                    if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = newListName.isNotBlank() && !saving, onClick = {
                    viewModel.addList(newListName) {
                        newListOpen = false
                        catalogOpen = false
                    }
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { newListOpen = false }) { Text(stringResource(R.string.cancel)) }
            })
    }
}
