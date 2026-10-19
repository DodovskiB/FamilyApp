package com.example.familyapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListScreen(viewModel: MainMenuViewModel, onOpenMenu: () -> Unit) {
    val lists by viewModel.lists.collectAsState()
    val saving by viewModel.listSaving.collectAsState()
    val error by viewModel.listError.collectAsState()
    var selectedListId by rememberSaveable { mutableStateOf<Long?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var renameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameName by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = lists.firstOrNull { it.id == selectedListId }

    if (selected != null) {
        ShoppingListDetails(viewModel, selected.id, selected.name) { selectedListId = null }
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Open menu") }
        Text("Shopping Lists", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newName, onValueChange = { newName = it }, label = { Text("New list name") },
                modifier = Modifier.weight(1f), singleLine = true, enabled = !saving
            )
            Button(enabled = newName.isNotBlank() && !saving, onClick = {
                viewModel.addList(newName) { newName = "" }
            }) { Text("Add") }
        }
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (lists.isEmpty()) Text("No lists yet. Create your first shopping list.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lists, key = { it.id }) { list ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().combinedClickable(
                        enabled = !saving,
                        onClick = {
                            viewModel.clearListError()
                            selectedListId = list.id
                        },
                        onLongClickLabel = "List actions",
                        onLongClick = {
                            viewModel.clearListError()
                            actionsId = list.id
                        }
                    ).padding(16.dp)) {
                        Text(list.name, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
    lists.firstOrNull { it.id == actionsId }?.let { list ->
        AlertDialog(
            onDismissRequest = { actionsId = null },
            title = { Text(list.name) },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    actionsId = null
                    renameId = list.id
                    renameName = list.name
                }) { Text("Rename") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { actionsId = null }) { Text("Cancel") }
                    TextButton(enabled = !saving, onClick = {
                        actionsId = null
                        deleteId = list.id
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
    if (renameId != null) {
        AlertDialog(
            onDismissRequest = { if (!saving) renameId = null },
            title = { Text("Rename list") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = renameName, onValueChange = { renameName = it },
                        label = { Text("List name") }, singleLine = true, enabled = !saving)
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = renameName.isNotBlank() && !saving, onClick = {
                    renameId?.let { id -> viewModel.renameList(id, renameName) { renameId = null } }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { renameId = null }) { Text("Cancel") }
            }
        )
    }
    lists.firstOrNull { it.id == deleteId }?.let { list ->
        AlertDialog(
            onDismissRequest = { if (!saving) deleteId = null },
            title = { Text("Delete list?") },
            text = {
                Column {
                    Text("Delete ${list.name} and its entries? Your catalog and other lists are kept.")
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.deleteList(list.id) { deleteId = null }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { deleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingListDetails(viewModel: MainMenuViewModel, listId: Long, name: String, onBack: () -> Unit) {
    val itemFlow = remember(viewModel, listId) { viewModel.listItems(listId) }
    val entries by itemFlow.collectAsState(initial = emptyList())
    val catalog by viewModel.catalogItems.collectAsState()
    val saving by viewModel.listSaving.collectAsState()
    val error by viewModel.listError.collectAsState()
    val photoSaving by viewModel.catalogSaving.collectAsState()
    val photoError by viewModel.catalogError.collectAsState()
    val photoActions = rememberProductPhotoActions(viewModel)
    var itemActionsId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    var viewingPhotoId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    var pickerOpen by rememberSaveable(listId) { mutableStateOf(false) }
    var search by rememberSaveable(listId) { mutableStateOf("") }
    var creating by rememberSaveable(listId) { mutableStateOf(false) }
    var title by rememberSaveable(listId) { mutableStateOf("") }
    var category by rememberSaveable(listId) { mutableStateOf("") }
    var removingId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    BackHandler(enabled = !pickerOpen && removingId == null && itemActionsId == null && viewingPhotoId == null) { onBack() }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back to lists") }
            Button(enabled = !saving, onClick = {
                viewModel.clearListError()
                search = ""
                pickerOpen = true
            }) { Text("Add product") }
        }
        Text(name, style = MaterialTheme.typography.headlineMedium)
        Text("${entries.count { it.checked }} of ${entries.size} purchased")
        Text("Hold a product for photos or removal.")
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (photoError != null) Text(photoError!!, color = MaterialTheme.colorScheme.error)
        if (entries.isEmpty()) Text("This list is empty. Add a product from your catalog.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().combinedClickable(
                        enabled = !saving && !photoSaving,
                        onClick = {}, onLongClickLabel = "Item actions",
                        onLongClick = {
                            viewModel.clearCatalogError()
                            viewModel.clearListError()
                            itemActionsId = item.id
                        }
                    ).padding(12.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Checkbox(checked = item.checked, enabled = !saving,
                                modifier = Modifier.semantics { contentDescription = "Purchased: ${item.title}" },
                                onCheckedChange = { viewModel.checkItem(item.id, it) })
                            ProductPhoto(catalog.firstOrNull { it.id == item.catalogItemId }?.photoFileName, item.title)
                            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None)
                                Text(item.category, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row {
                                TextButton(enabled = !saving && item.qty > 1,
                                    modifier = Modifier.semantics { contentDescription = "Decrease quantity: ${item.title}" },
                                    onClick = { viewModel.changeQuantity(item.id, -1) }) { Text("−") }
                                Text(item.qty.toString(), Modifier.padding(top = 14.dp))
                                TextButton(enabled = !saving && item.qty < Int.MAX_VALUE,
                                    modifier = Modifier.semantics { contentDescription = "Increase quantity: ${item.title}" },
                                    onClick = { viewModel.changeQuantity(item.id, 1) }) { Text("+") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickerOpen && !creating) {
        AlertDialog(
            onDismissRequest = { if (!saving) pickerOpen = false },
            title = { Text("Choose a product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        label = { Text("Search products") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving
                    )
                    val suggestions = if (search.isBlank()) emptyList() else
                        catalog.filter { it.title.startsWith(search.trim(), ignoreCase = true) }
                    if (catalog.isEmpty()) Text("Your catalog is empty. Create a product below.")
                    else if (search.isBlank()) Text("Type a product name to see suggestions.")
                    else if (suggestions.isEmpty()) Text("No matching products. You can create one below.")
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(suggestions, key = { it.id }) { product ->
                            Row(Modifier.fillMaxWidth().clickable(enabled = !saving) {
                                viewModel.addProductToList(listId, product.id) { pickerOpen = false }
                            }.padding(vertical = 12.dp)) {
                                ProductPhoto(product.photoFileName, product.title)
                                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(product.title, style = MaterialTheme.typography.titleMedium)
                                    Text(product.category)
                                }
                            }
                        }
                    }
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.clearListError()
                    title = search.trim()
                    category = ""
                    creating = true
                }) { Text("New product") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { pickerOpen = false }) { Text("Cancel") }
            }
        )
    }
    if (pickerOpen && creating) {
        AlertDialog(
            onDismissRequest = { if (!saving) creating = false },
            title = { Text("New product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it },
                        label = { Text("Product name") }, singleLine = true, enabled = !saving)
                    OutlinedTextField(value = category, onValueChange = { category = it },
                        label = { Text("Category") }, singleLine = true, enabled = !saving,
                        supportingText = { Text("Leave blank to use Other") })
                    if (catalog.any { it.title.equals(title.trim(), ignoreCase = true) }) {
                        Text("This name already exists. Cancel to choose it, or save to create a separate product.")
                    }
                    Text("Saved to your catalog and added to this list.")
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && !saving, onClick = {
                    viewModel.createProductForList(listId, title, category) {
                        creating = false
                        pickerOpen = false
                    }
                }) { Text("Save and add") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { creating = false }) { Text("Cancel") }
            }
        )
    }
    entries.firstOrNull { it.id == itemActionsId }?.let { item ->
        AlertDialog(onDismissRequest = { itemActionsId = null }, title = { Text(item.title) },
            text = {
                Column {
                    catalog.firstOrNull { it.id == item.catalogItemId }?.let { product ->
                        PhotoMenuOptions(product, photoActions, !saving && !photoSaving,
                            onClose = { itemActionsId = null }, onView = { viewingPhotoId = product.id },
                            onRemove = { viewModel.removeCatalogPhoto(product.id) })
                    }
                    TextButton(enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                        itemActionsId = null
                        removingId = item.id
                    }) { Text("Remove from list") }
                }
            },
            confirmButton = { TextButton(onClick = { itemActionsId = null }) { Text("Cancel") } })
    }
    catalog.firstOrNull { it.id == viewingPhotoId }?.let { product ->
        ProductPhotoDialog(product) { viewingPhotoId = null }
    }
    entries.firstOrNull { it.id == removingId }?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) removingId = null },
            title = { Text("Remove product?") },
            text = {
                Column {
                    Text("Remove ${item.title} from this list? The catalog and other lists are kept.")
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.removeListItem(item.id) { removingId = null }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { removingId = null }) { Text("Cancel") }
            }
        )
    }
}
