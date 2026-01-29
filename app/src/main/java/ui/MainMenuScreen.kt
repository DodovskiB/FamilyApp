@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.familyapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.familyapp.data.entity.ListEntity

@Composable
fun MainMenuScreen(
    lists: List<ListEntity>,
    onOpenList: (listId: Long, listName: String) -> Unit,
    onAddList: (name: String) -> Unit,
    onRenameList: (listId: Long, newName: String) -> Unit,
    onDeleteList: (listId: Long) -> Unit,
) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ListEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ListEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("FamilyApp") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add list")
            }
        }
    ) { padding ->
        if (lists.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Нема листи уште. Додади нова со +")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lists, key = { it.id }) { list ->
                    val protected = isProtectedList(list.name)

                    ListCard(
                        name = list.name,
                        subtitle = if (protected) "Default листа" else "Custom листа",
                        leadingIcon = iconForListName(list.name),
                        protected = protected,
                        onClick = { onOpenList(list.id, list.name) },
                        onRename = { renameTarget = list },
                        onDelete = { deleteTarget = list }
                    )
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(
            title = "Нова листа",
            initial = "",
            confirmText = "Add",
            onDismiss = { showAdd = false },
            onConfirm = { name ->
                onAddList(name)
                showAdd = false
            }
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "Rename листа",
            initial = target.name,
            confirmText = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRenameList(target.id, newName)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete листа") },
            text = { Text("Сигурно сакаш да ја избришеш „${target.name}“?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteList(target.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ListCard(
    name: String,
    subtitle: String,
    leadingIcon: @Composable () -> Unit,
    protected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon()
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename")
            }
            IconButton(
                onClick = onDelete,
                enabled = !protected
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Име") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun isProtectedList(name: String): Boolean {
    val n = name.trim().lowercase()
    return n == "дома" || n == "vikendica" || n == "викендица" || n == "home"
}

@Composable
private fun iconForListName(name: String): @Composable () -> Unit {
    val n = name.trim().lowercase()
    return when {
        n == "дома" || n == "home" -> { { Icon(Icons.Default.Home, contentDescription = null) } }
        n == "викендица" || n == "vikendica" -> { { Icon(Icons.Default.Cabin, contentDescription = null) } }
        else -> { { Icon(Icons.Default.List, contentDescription = null) } }
    }
}
