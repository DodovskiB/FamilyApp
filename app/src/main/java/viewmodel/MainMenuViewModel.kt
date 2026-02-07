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
        repo.observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ensureDefaults() {
        viewModelScope.launch {
            val existing = repo.getLists()
            if (existing.isNotEmpty()) return@launch

            // Прва Дома, втора Викендица
            repo.addList("Дома")
            repo.addList("Викендица")
        }
    }

    fun addList(name: String) {
        viewModelScope.launch {
            repo.addList(name)
        }
    }

    /**
     * ✅ Привремено (за да нема "Unresolved reference" и да компајлира проектот).
     * Во следен чекор ќе ги врземе со Repo/Dao за да работат навистина.
     */
    fun renameList(listId: Long, newName: String) {
        // TODO: ќе имплементираме преку Room (ListDao update)
    }

    fun deleteList(listId: Long) {
        // TODO: ќе имплементираме преку Room (ListDao delete или soft-delete)
    }
}
