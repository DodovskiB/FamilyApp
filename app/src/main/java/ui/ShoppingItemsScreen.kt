package com.example.familyapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingItemsScreen(viewModel: MainMenuViewModel, onBack: () -> Unit) {
    val products by viewModel.catalogItems.collectAsState()
    val saving by viewModel.catalogSaving.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val photoActions = rememberProductPhotoActions(viewModel)
    var viewingPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var actionsId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }

    BackHandler(enabled = !editorOpen && actionsId == null && deletingId == null && viewingPhotoId == null) { onBack() }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            Button(onClick = {
                editingId = null
                title = ""
                category = ""
                viewModel.clearCatalogError()
                editorOpen = true
            }, enabled = !saving) { Text("Add product") }
        }
        Text("Shopping Items", style = MaterialTheme.typography.headlineMedium)
        Text("Hold a product for photos, editing or deletion.")
        if (error != null && !editorOpen && deletingId == null) Text(error!!, color = MaterialTheme.colorScheme.error)
        if (products.isEmpty()) Text("No products yet. Add your first product.")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { product ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            enabled = !saving,
                            onClick = {
                                editingId = product.id
                                title = product.title
                                category = product.category
                                viewModel.clearCatalogError()
                                editorOpen = true
                            },
                            onLongClickLabel = "Product actions",
                            onLongClick = { actionsId = product.id }
                        ).padding(16.dp)
                    ) {
                        ProductPhoto(product.photoFileName, product.title)
                        Text(product.title, style = MaterialTheme.typography.titleMedium)
                        Text(product.category, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (editorOpen) {
        AlertDialog(
            onDismissRequest = { if (!saving) editorOpen = false },
            title = { Text(if (editingId == null) "Add product" else "Edit product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Product name") }, singleLine = true,
                        enabled = !saving, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category, onValueChange = { category = it },
                        label = { Text("Category") }, singleLine = true,
                        supportingText = { Text("Leave blank to use Other") },
                        enabled = !saving, modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank() && !saving, onClick = {
                    val existing = products.firstOrNull { it.id == editingId }
                    if (editingId == null || existing != null) {
                        viewModel.saveCatalogItem(existing, title, category) { editorOpen = false }
                    }
                }) { Text(if (saving) "Saving..." else "Save") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { editorOpen = false }) { Text("Cancel") }
            }
        )
    }

    products.firstOrNull { it.id == actionsId }?.let { product ->
        AlertDialog(
            onDismissRequest = { actionsId = null },
            title = { Text(product.title) },
            text = {
                Column {
                    TextButton(enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                        actionsId = null
                        editingId = product.id
                        title = product.title
                        category = product.category
                        viewModel.clearCatalogError()
                        editorOpen = true
                    }) { Text("Edit") }
                    PhotoMenuOptions(product, photoActions, !saving, onClose = { actionsId = null },
                        onView = { viewingPhotoId = product.id },
                        onRemove = { viewModel.removeCatalogPhoto(product.id) })
                    TextButton(enabled = !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                        actionsId = null
                        viewModel.clearCatalogError()
                        deletingId = product.id
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionsId = null }) { Text("Cancel") }
            }
        )
    }

    products.firstOrNull { it.id == deletingId }?.let { product ->
        AlertDialog(
            onDismissRequest = { if (!saving) deletingId = null },
            title = { Text("Delete product?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remove ${product.title} from the catalog? Existing shopping list items are kept.")
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    viewModel.deleteCatalogItem(product.id) { deletingId = null }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { deletingId = null }) { Text("Cancel") }
            }
        )
    }
    products.firstOrNull { it.id == viewingPhotoId }?.let { product ->
        ProductPhotoDialog(product) { viewingPhotoId = null }
    }
}
