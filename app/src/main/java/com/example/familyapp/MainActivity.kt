package com.example.familyapp

import repo.RoomFamilyRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.ui.MainMenuScreen
import com.example.familyapp.ui.ShoppingItemsScreen
import com.example.familyapp.ui.ShoppingListScreen
import com.example.familyapp.ui.theme.FamilyAppTheme
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FamilyAppTheme {
                var screen by remember { mutableStateOf(Screen.MainMenu) }

                // DB
                val db = AppDatabase.getInstance(applicationContext)

                // REPO
                val repo = RoomFamilyRepository(
                    listDao = db.listDao(),
                    itemDao = db.itemDao()
                )

                // VIEWMODEL
                val mainMenuViewModel: MainMenuViewModel = viewModel(
                    factory = MainMenuViewModelFactory(repository = repo)
                )

                when (screen) {
                    Screen.MainMenu -> {
                        MainMenuScreen(
                            viewModel = mainMenuViewModel,
                            onShoppingList = { screen = Screen.ShoppingList },
                            onShoppingItems = { screen = Screen.ShoppingItems }
                        )
                    }

                    Screen.ShoppingList -> {
                        ShoppingListScreen(
                            viewModel = mainMenuViewModel,
                            onBack = { screen = Screen.MainMenu }
                        )
                    }

                    Screen.ShoppingItems -> {
                        ShoppingItemsScreen(
                            onBack = { screen = Screen.MainMenu }
                        )
                    }
                }
            }
        }
    }
}
