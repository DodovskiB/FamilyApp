package com.example.familyapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.familyapp.data.ProductLinks
import com.example.familyapp.data.entity.CatalogItemEntity
import com.example.familyapp.viewmodel.MainMenuViewModel

@Composable
fun ProductLinkMenuOptions(product: CatalogItemEntity, enabled: Boolean,
                           onClose: () -> Unit, onEdit: () -> Unit, onOpenFailure: () -> Unit,
                           onOpenLink: (String) -> Unit) {
    TextButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = { onClose(); onEdit() }) {
        Text(if (product.linkUrl == null) "Add link" else "Edit link")
    }
    product.linkUrl?.let { url ->
        TextButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
            onClose()
            try { onOpenLink(url) } catch (_: Exception) { onOpenFailure() }
        }) { Text("Open link") }
    }
}

@Composable
fun ProductLinkDialog(product: CatalogItemEntity, viewModel: MainMenuViewModel, onDismiss: () -> Unit) {
    var url by rememberSaveable(product.id) { mutableStateOf(product.linkUrl.orEmpty()) }
    val saving by viewModel.catalogSaving.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val invalid = runCatching { ProductLinks.normalize(url) }.isFailure
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Link for ${product.title}") },
        text = {
            Column {
                OutlinedTextField(value = url, onValueChange = { url = it; viewModel.clearCatalogError() },
                    label = { Text("Product link") }, singleLine = true, enabled = !saving,
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = invalid,
                    supportingText = {
                        Text(if (invalid) "Enter a valid http or https website link." else "For example, an Amazon or eBay product page.")
                    })
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                if (product.linkUrl != null) {
                    TextButton(enabled = !saving, onClick = {
                        viewModel.setCatalogLink(product.id, "", onDismiss)
                    }) { Text("Remove link") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving && !invalid && (url.isNotBlank() || product.linkUrl != null), onClick = {
                viewModel.setCatalogLink(product.id, url, onDismiss)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}
