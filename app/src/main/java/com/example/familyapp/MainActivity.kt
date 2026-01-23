package com.example.familyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.familyapp.ui.theme.FamilyAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProductChecklist()
                }
            }
        }
    }
}

@Composable
fun ProductChecklist() {
    // Листа на продукти
    val products = remember { mutableStateListOf(
        "Прашок за перење",
        "Влошки за жени",
        "Млеко",
        "Леб",
        "Зеленчук"
    ) }

    // Чекнати/нечекнати продукти
    val checkedStates = remember { mutableStateMapOf<String, Boolean>() }

    var newProduct by remember { mutableStateOf("") } // За нов продукт

    Column(modifier = Modifier.padding(16.dp)) {
        // Поле за внес на нов продукт + копче
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = newProduct,
                onValueChange = { newProduct = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Внеси нов продукт") }
            )
            Button(onClick = {
                if (newProduct.isNotBlank()) {
                    products.add(newProduct)
                    newProduct = ""
                }
            }) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = product, style = MaterialTheme.typography.bodyLarge)
                    val checked = checkedStates[product] ?: false
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checkedStates[product] = it }
                    )
                }
            }
        }
    }
}
