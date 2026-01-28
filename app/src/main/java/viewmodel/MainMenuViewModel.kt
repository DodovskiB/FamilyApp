package com.example.familyapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.repo.RoomFamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMenuViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app.applicationContext)
    private val repo = RoomFamilyRepository(db.listDao(), db.itemDao())

    // Листите како StateFlow за UI
    val lists: StateFlow<List<com.example.familyapp.data.entity.ListEntity>> =
        repo.observeLists().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun seedDefaultsIfEmpty() {
        viewModelScope.launch {
            if (lists.value.isEmpty()) {
                withContext(Dispatchers.IO) {
                    repo.addList("Дома", sortOrder = 0)
                    repo.addList("Викендица", sortOrder = 1)
                }
            }
        }
    }

    fun addList(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.addList(trimmed) // автоматски оди по default листите
            }
        }
    }
}
