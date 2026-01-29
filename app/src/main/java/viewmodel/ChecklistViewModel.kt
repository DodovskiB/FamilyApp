package com.example.familyapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.repo.FamilyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val KIND_LIST = 0
private const val KIND_CATALOG = 1

class ChecklistViewModel(
    private val repo: FamilyRepository,
    private val listId: Long,
    private val kind: Int
) : ViewModel() {

    val items: StateFlow<List<ItemEntity>> =
        repo.observeItems(listId, kind)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addOrIncrement(title: String) {
        viewModelScope.launch {
            repo.addItem(listId, title, kind)
        }
    }

    fun toggleChecked(item: ItemEntity) {
        viewModelScope.launch {
            repo.toggleChecked(item.id, !item.checked)
        }
    }

    fun incQty(item: ItemEntity) {
        viewModelScope.launch {
            repo.setQty(item.id, item.qty + 1)
        }
    }

    fun decQty(item: ItemEntity) {
        viewModelScope.launch {
            repo.setQty(item.id, item.qty - 1) // <=0 ќе го тргне (soft delete)
        }
    }

    fun deactivateItem(item: ItemEntity) {
        viewModelScope.launch {
            repo.deleteItemSoft(item.id)
        }
    }
}
