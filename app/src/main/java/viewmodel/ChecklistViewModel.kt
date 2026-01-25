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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChecklistViewModel(
    app: Application,
    private val listId: Long
) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = RoomFamilyRepository(db.listDao(), db.itemDao())

    private val _items = MutableStateFlow<List<ItemEntity>>(emptyList())
    val items: StateFlow<List<ItemEntity>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val loaded = withContext(Dispatchers.IO) { repo.getItems(listId) }
            _items.value = loaded
            _loading.value = false
        }
    }

    fun addItem(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.addItem(listId, clean) }
            refresh()
        }
    }

    fun toggleChecked(item: ItemEntity, checked: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateItem(item.copy(isChecked = checked))
            }
            refresh()
        }
    }
}

class ChecklistViewModelFactory(
    private val app: Application,
    private val listId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChecklistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChecklistViewModel(app, listId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
