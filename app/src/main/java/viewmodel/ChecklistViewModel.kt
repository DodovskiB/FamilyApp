package com.example.familyapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.repo.RoomFamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChecklistViewModel(
    app: Application,
    private val listId: Long
) : AndroidViewModel(app) {

    private val repo = RoomFamilyRepository(
        AppDatabase.get(app).listDao(),
        AppDatabase.get(app).itemDao()
    )

    private val selectedKind = MutableStateFlow(0) // 0=Shopping list, 1=Shopping items

    val items: StateFlow<List<ItemEntity>> =
        selectedKind
            .flatMapLatest { kind: Int -> repo.observeItems(listId, kind) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTab(kind: Int) {
        selectedKind.value = kind
    }

    fun addItem(title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.addItem(listId, trimmed, selectedKind.value)
            }
        }
    }

    fun toggleChecked(item: ItemEntity, checked: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateItem(item.copy(isChecked = checked))
            }
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.deactivateItem(item)
            }
        }
    }
}

class ChecklistViewModelFactory(
    private val app: Application,
    private val listId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChecklistViewModel(app, listId) as T
    }
}
