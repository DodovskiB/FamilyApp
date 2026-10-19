package com.example.familyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.ui.FamilyAppRoot
import com.example.familyapp.ui.theme.FamilyAppTheme
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import repo.RoomFamilyRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyAppTheme {
                val db = AppDatabase.getInstance(applicationContext)
                val repo = remember(db) { RoomFamilyRepository(db) }
                val mainMenuViewModel: MainMenuViewModel = viewModel(
                    factory = MainMenuViewModelFactory(repository = repo)
                )
                FamilyAppRoot(mainMenuViewModel)
            }
        }
    }
}
