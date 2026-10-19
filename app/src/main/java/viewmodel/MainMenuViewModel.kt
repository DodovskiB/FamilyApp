package com.example.familyapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import repo.FamilyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainMenuViewModel(
    private val repository: FamilyRepository
) : ViewModel() {

    val lists: StateFlow<List<ListEntity>> =
        repository.observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addList(name: String) = viewModelScope.launch {
        repository.addList(name)
    }

    fun renameList(listId: Long, newName: String) = viewModelScope.launch {
        repository.renameList(listId, newName)
    }

    fun deleteList(listId: Long) = viewModelScope.launch {
        repository.deleteList(listId)
    }

    fun observeItemsByList(listId: Long): StateFlow<List<ItemEntity>> =
        repository.observeItems(listId, kind = 0)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(
        listId: Long,
        name: String
    ) = viewModelScope.launch {
        repository.addItem(
            listId = listId,
            title = name,
            kind = 0
        )
    }
}
