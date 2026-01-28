package com.example.familyapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.repo.RoomFamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class ChecklistViewModel(
    app: Application,
    private val listId: Long
) : AndroidViewModel(app) {

    private val repo = RoomFamilyRepository(
        AppDatabase.get(app).listDao(),
        AppDatabase.get(app).itemDao()
    )

    val items: StateFlow<List<ItemEntity>> =
        repo.observeItems(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(title: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.addOrIncrementItem(listId, title)
            }
        }
    }

    fun toggleChecked(item: ItemEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateItem(item.copy(isChecked = !item.isChecked))
            }
        }
    }

    fun incQty(item: ItemEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateItem(item.copy(quantity = item.quantity + 1))
            }
        }
    }

    fun decQty(item: ItemEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val next = item.quantity - 1
                if (next <= 0) repo.deactivateItem(item)
                else repo.updateItem(item.copy(quantity = max(1, next)))
            }
        }
    }

    fun rename(item: ItemEntity, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateItem(item.copy(title = trimmed))
            }
        }
    }

    fun delete(item: ItemEntity) {
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
