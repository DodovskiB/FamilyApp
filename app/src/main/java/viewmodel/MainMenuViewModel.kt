package com.example.familyapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.data.repo.RoomFamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMenuViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RoomFamilyRepository(
        AppDatabase.get(app).listDao(),
        AppDatabase.get(app).itemDao()
    )

    val lists: StateFlow<List<ListEntity>> =
        repo.observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addList(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.addList(trimmed)
            }
        }
    }
}
