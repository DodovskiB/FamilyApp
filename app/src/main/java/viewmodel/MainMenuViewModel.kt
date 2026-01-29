package com.example.familyapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.data.repo.FamilyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainMenuViewModel(
    private val repo: FamilyRepository
) : ViewModel() {

    val lists: StateFlow<List<ListEntity>> =
        repo.observeLists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ensureDefaults() {
        viewModelScope.launch {
            val existing = repo.getLists()
            if (existing.isNotEmpty()) return@launch

            // ✅ прва Дома, втора Викендица (со sortOrder)
            repo.addList("Дома", sortOrder = 0)
            repo.addList("Викендица", sortOrder = 1)
        }
    }

    fun addList(name: String) {
        viewModelScope.launch {
            val current = repo.getLists()
            val order = current.size
            repo.addList(name, sortOrder = order)
        }
    }
}
