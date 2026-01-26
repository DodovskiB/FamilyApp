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

    private val repo = RoomFamilyRepository(
        AppDatabase.get(app).listDao(),
        AppDatabase.get(app).itemDao()
    )

    private val _items = MutableStateFlow<List<ItemEntity>>(emptyList())
    val items: StateFlow<List<ItemEntity>> = _items

    private val _selectedKind = MutableStateFlow(0) // 0 = Shopping list, 1 = Shopping items

    init {
        refresh()
    }

    fun setTab(kind: Int) {
        _selectedKind.value = kind
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                repo.getItems(listId, _selectedKind.value)
            }
            _items.value = data
        }
    }

    fun addItem(title: String) {
        if (title.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.addItem(listId, title.trim(), _selectedKind.value)
            }
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
        return ChecklistViewModel(app, listId) as T
    }
}
