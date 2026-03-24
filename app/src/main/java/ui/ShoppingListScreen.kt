package com.example.familyapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel

@Composable
fun ShoppingListScreen(
    viewModel: MainMenuViewModel,
    onBack: () -> Unit
) {
    val lists by viewModel.lists.collectAsState()

    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Text("Shopping Lists")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New list name") }
            )
            Button(
                onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        viewModel.addList(name)
                        newName = ""
                    }
                }
            ) { Text("Add") }
        }

        LazyColumn {
            items(lists) { list ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(list.name)
                    Button(onClick = { viewModel.deleteList(list.id) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
