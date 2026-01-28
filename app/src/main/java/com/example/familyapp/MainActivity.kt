package com.example.familyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                    AppRoot()
                }
            }
        }
    }
}

/* -------------------- SCREEN STATE -------------------- */

enum class Screen {
    MAIN_MENU,
    SHOPPING_LIST,
    SHOPPING_ITEMS
}

/* -------------------- ROOT -------------------- */

@Composable
fun AppRoot() {

    // ❗ НЕ rememberSaveable (enum не е Bundle-safe)
    var screen by remember { mutableStateOf(Screen.MAIN_MENU) }

    when (screen) {
        Screen.MAIN_MENU -> MainMenuScreen(
            onShoppingList = { screen = Screen.SHOPPING_LIST },
            onShoppingItems = { screen = Screen.SHOPPING_ITEMS }
        )

        Screen.SHOPPING_LIST -> SimpleScreen(
            title = "Shopping List",
            onBack = { screen = Screen.MAIN_MENU }
        )

        Screen.SHOPPING_ITEMS -> SimpleScreen(
            title = "Shopping Items",
            onBack = { screen = Screen.MAIN_MENU }
        )
    }
}

/* -------------------- MAIN MENU -------------------- */

@Composable
fun MainMenuScreen(
    onShoppingList: () -> Unit,
    onShoppingItems: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Family App",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShoppingList
        ) {
            Text("Shopping List")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShoppingItems
        ) {
            Text("Shopping Items")
        }
    }
}

/* -------------------- PLACEHOLDER SCREEN -------------------- */

@Composable
fun SimpleScreen(
    title: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
