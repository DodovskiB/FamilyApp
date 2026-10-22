package com.example.familyapp.ui

import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.sync.SharingState
import com.example.familyapp.data.entity.CatalogItemEntity
import com.example.familyapp.data.entity.ItemEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListScreen(viewModel: MainMenuViewModel, onOpenMenu: () -> Unit,
                       sharing: SharingState? = null, onShare: (Long) -> Unit = {}, onNewList: () -> Unit = {}) {
    val lists by viewModel.lists.collectAsState()
    val saving by viewModel.listSaving.collectAsState()
    val error by viewModel.listError.collectAsState()
    var selectedListId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameName by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = lists.firstOrNull { it.id == selectedListId }

    if (selected != null) {
        ShoppingListDetails(viewModel, selected.id, localizedListName(selected.name)) { selectedListId = null }
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.open_menu)) }
            Button(enabled = !saving, onClick = onNewList) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.new_list))
            }
        }
        Text(stringResource(R.string.shopping_lists_title), style = MaterialTheme.typography.headlineMedium)
        if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
        if (lists.isEmpty()) Text(stringResource(R.string.no_lists))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lists, key = { it.id }) { list ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().combinedClickable(
                        enabled = !saving,
                        onClick = {
                            viewModel.clearListError()
                            selectedListId = list.id
                        },
                        onLongClickLabel = stringResource(R.string.list_actions),
                        onLongClick = {
                            viewModel.clearListError()
                            actionsId = list.id
                        }
                    ).padding(16.dp)) {
                        Text(localizedListName(list.name), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
    lists.firstOrNull { it.id == actionsId }?.let { list ->
        val displayedName = localizedListName(list.name)
        AlertDialog(
            onDismissRequest = { actionsId = null },
            title = { Text(localizedListName(list.name)) },
            text = {
                Column {
                if (sharing != null && sharing.access[list.id]?.role != "admin") {
                    TextButton(enabled = sharing.ready && !sharing.busy, onClick = { actionsId = null; onShare(list.id) }) {
                        Text(stringResource(R.string.share_members))
                    }
                }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    actionsId = null
                    renameId = list.id
                    renameName = displayedName
                }) { Text(stringResource(R.string.rename)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { actionsId = null }) { Text(stringResource(R.string.cancel)) }
                    if (sharing == null || (sharing.ready && sharing.access[list.id]?.role != "admin")) TextButton(enabled = !saving, onClick = {
                        actionsId = null
                        deleteId = list.id
                    }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
    if (renameId != null) {
        val originalName = lists.firstOrNull { it.id == renameId }?.let { localizedListName(it.name) }
        AlertDialog(
            onDismissRequest = { if (!saving) renameId = null },
            title = { Text(stringResource(R.string.rename_list)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = renameName, onValueChange = { renameName = it },
                        label = { Text(stringResource(R.string.list_name)) }, singleLine = true, enabled = !saving)
                    if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = renameName.isNotBlank() && !saving, onClick = {
                    if (renameName.trim() == originalName) renameId = null
                    else renameId?.let { id -> viewModel.renameList(id, renameName) { renameId = null } }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { renameId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    lists.firstOrNull { it.id == deleteId }?.let { list ->
        AlertDialog(
            onDismissRequest = { if (!saving) deleteId = null },
            title = { Text(stringResource(R.string.delete_list)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_list_body, localizedListName(list.name)))
                    if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.deleteList(list.id) { deleteId = null }
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingListDetails(viewModel: MainMenuViewModel, listId: Long, name: String, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val itemFlow = remember(viewModel, listId) { viewModel.listItems(listId) }
    val entries by itemFlow.collectAsState(initial = emptyList())
    val catalog by viewModel.catalogItems.collectAsState()
    val saving by viewModel.listSaving.collectAsState()
    val error by viewModel.listError.collectAsState()
    val repeatProduct by viewModel.repeatProductId.collectAsState()
    var repeatQuantity by rememberSaveable(repeatProduct) { mutableStateOf("1") }
    val photoSaving by viewModel.catalogSaving.collectAsState()
    val photoError by viewModel.catalogError.collectAsState()
    val photoActions = rememberProductPhotoActions(viewModel, listEntry = true)
    var itemActionsId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    var viewingPhotoId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    var linkingProductId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    var pickerOpen by rememberSaveable(listId) { mutableStateOf(false) }
    var search by rememberSaveable(listId) { mutableStateOf("") }
    var creating by rememberSaveable(listId) { mutableStateOf(false) }
    var title by rememberSaveable(listId) { mutableStateOf("") }
    var category by rememberSaveable(listId) { mutableStateOf("") }
    var removingId by rememberSaveable(listId) { mutableStateOf<Long?>(null) }
    var clearOpen by rememberSaveable(listId) { mutableStateOf(false) }
    BackHandler(enabled = !clearOpen && !pickerOpen && removingId == null && itemActionsId == null && viewingPhotoId == null && linkingProductId == null) { onBack() }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back_lists)) }
            Button(enabled = !saving, onClick = {
                viewModel.clearListError()
                search = ""
                pickerOpen = true
            }) { Text(stringResource(R.string.add_product)) }
        }
        Text(name, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.purchased_count, entries.count { it.checked }, entries.size))
        Text(stringResource(R.string.hold_list_product))
        if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
        if (photoError != null) Text(localizedMessage(photoError!!), color = MaterialTheme.colorScheme.error)
        if (entries.isEmpty()) Text(stringResource(R.string.empty_list))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false, true).forEach { purchased ->
                val section = entries.filter { it.checked == purchased }
                if (section.isNotEmpty()) item(key = if (purchased) "purchased-heading" else "outstanding-heading") {
                    Text(if (purchased) stringResource(R.string.purchased) else stringResource(R.string.to_buy), style = MaterialTheme.typography.titleLarge)
                }
                items(section, key = { it.id }) { item ->
                val purchasedDescription = stringResource(R.string.purchased_description, item.title)
                val decreaseDescription = stringResource(R.string.decrease_description, item.title)
                val increaseDescription = stringResource(R.string.increase_description, item.title)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().combinedClickable(
                        enabled = !saving && !photoSaving,
                        onClick = {}, onLongClickLabel = stringResource(R.string.item_actions),
                        onLongClick = {
                            viewModel.clearCatalogError()
                            viewModel.clearListError()
                            itemActionsId = item.id
                        }
                    ).padding(12.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Checkbox(checked = item.checked, enabled = !saving,
                                modifier = Modifier.semantics { contentDescription = purchasedDescription },
                                onCheckedChange = { viewModel.checkItem(item.id, it) })
                            ProductPhoto(item.photoFileName, item.title)
                            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None)
                                Text(item.category, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row {
                                TextButton(enabled = !saving && item.qty > 1,
                                    modifier = Modifier.semantics { contentDescription = decreaseDescription },
                                    onClick = { viewModel.changeQuantity(item.id, -1) }) { Text("−") }
                                Text(item.qty.toString(), Modifier.padding(top = 14.dp))
                                TextButton(enabled = !saving && item.qty < Int.MAX_VALUE,
                                    modifier = Modifier.semantics { contentDescription = increaseDescription },
                                    onClick = { viewModel.changeQuantity(item.id, 1) }) { Text("+") }
                            }
                        }
                    }
                }
            }
            }
            if (entries.any { it.checked }) item(key = "clear-purchased-footer") {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider()
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = {
                        viewModel.clearListError(); clearOpen = true
                    }) { Text(stringResource(R.string.clear_purchased)) }
                }
            }
        }
    }

    if (clearOpen) {
        AlertDialog(onDismissRequest = { if (!saving) clearOpen = false },
            title = { Text(stringResource(R.string.clear_purchased_confirm)) },
            text = { Column {
                Text(stringResource(R.string.clear_body, name))
                if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(enabled = !saving, onClick = {
                viewModel.clearPurchased(listId) { clearOpen = false }
            }) { Text(stringResource(R.string.clear_purchased)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { clearOpen = false }) { Text(stringResource(R.string.cancel)) } })
    }
    if (repeatProduct != null) {
        val quantity = repeatQuantity.toIntOrNull()?.takeIf { it > 0 }
        AlertDialog(onDismissRequest = { if (!saving) viewModel.cancelRepeatPurchase() },
            title = { Text(stringResource(R.string.buy_again_confirm)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.repeat_body, catalog.firstOrNull { it.id == repeatProduct }?.title ?: stringResource(R.string.this_product)))
                OutlinedTextField(repeatQuantity, { repeatQuantity = it.filter(Char::isDigit).take(10) },
                    label = { Text(stringResource(R.string.new_quantity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, enabled = !saving, isError = quantity == null)
                if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(enabled = !saving && quantity != null, onClick = {
                viewModel.addProductToList(listId, repeatProduct!!, quantity) { pickerOpen = false }
            }) { Text(stringResource(R.string.buy_again)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { viewModel.cancelRepeatPurchase() }) { Text(stringResource(R.string.cancel)) } })
    }
    if (pickerOpen && !creating && repeatProduct == null) {
        AlertDialog(
            onDismissRequest = { if (!saving) pickerOpen = false },
            title = { Text(stringResource(R.string.choose_product)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        label = { Text(stringResource(R.string.search_products)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving
                    )
                    val suggestions = if (search.isBlank()) emptyList() else
                        catalog.filter { it.title.startsWith(search.trim(), ignoreCase = true) }
                    if (catalog.isEmpty()) Text(stringResource(R.string.empty_catalog))
                    else if (search.isBlank()) Text(stringResource(R.string.search_hint))
                    else if (suggestions.isEmpty()) Text(stringResource(R.string.no_matches))
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
                    if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.clearListError()
                    title = search.trim()
                    category = ""
                    creating = true
                }) { Text(stringResource(R.string.new_product)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { pickerOpen = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (pickerOpen && creating) {
        AlertDialog(
            onDismissRequest = { if (!saving) creating = false },
            title = { Text(stringResource(R.string.new_product)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it },
                        label = { Text(stringResource(R.string.product_name)) }, singleLine = true, enabled = !saving)
                    OutlinedTextField(value = category, onValueChange = { category = it },
                        label = { Text(stringResource(R.string.category)) }, singleLine = true, enabled = !saving,
                        supportingText = { Text(stringResource(R.string.category_hint)) })
                    if (catalog.any { it.title.equals(title.trim(), ignoreCase = true) }) {
                        Text(stringResource(R.string.name_exists))
                    }
                    Text(stringResource(R.string.save_catalog_list))
                    if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && !saving, onClick = {
                    viewModel.createProductForList(listId, title, category) {
                        creating = false
                        pickerOpen = false
                    }
                }) { Text(stringResource(R.string.save_add)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { creating = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    entries.firstOrNull { it.id == itemActionsId }?.let { item ->
        AlertDialog(onDismissRequest = { itemActionsId = null }, title = { Text(item.title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val product = item.mediaDisplay()
                    ProductMediaMenuOptions(product, photoActions, !saving && !photoSaving,
                        onClose = { itemActionsId = null }, onView = { viewingPhotoId = item.id },
                        onRemove = { viewModel.removeItemPhoto(item.id) }, hasRemotePhoto = item.photoRemotePath != null,
                        onEditLink = { viewModel.clearCatalogError(); linkingProductId = item.id },
                        onOpenFailure = { viewModel.linkOpenFailed() }, onOpenLink = { uriHandler.openUri(it) })
                    TextButton(enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                        itemActionsId = null
                        removingId = item.id
                    }) { Text(stringResource(R.string.remove_from_list)) }
                }
            },
            confirmButton = { TextButton(onClick = { itemActionsId = null }) { Text(stringResource(R.string.cancel)) } })
    }
    entries.firstOrNull { it.id == viewingPhotoId }?.let { item ->
        ProductPhotoDialog(item.mediaDisplay()) { viewingPhotoId = null }
    }
    entries.firstOrNull { it.id == linkingProductId }?.let { item ->
        ProductLinkDialog(item.mediaDisplay(), viewModel, listEntry = true) { linkingProductId = null }
    }
    entries.firstOrNull { it.id == removingId }?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) removingId = null },
            title = { Text(stringResource(R.string.remove_product)) },
            text = {
                Column {
                    Text(stringResource(R.string.remove_body, item.title))
                    if (error != null) Text(localizedMessage(error!!), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.removeListItem(item.id) { removingId = null }
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { removingId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

private fun ItemEntity.mediaDisplay() = CatalogItemEntity(id = id, title = title, category = category,
    photoFileName = photoFileName, linkUrl = linkUrl)
