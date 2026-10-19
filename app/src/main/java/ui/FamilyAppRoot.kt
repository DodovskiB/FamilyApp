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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
}
