package com.example.familyapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.data.repo.RoomFamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMenuViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = RoomFamilyRepository(db.listDao(), db.itemDao())

    private val _lists = MutableStateFlow<List<ListEntity>>(emptyList())
    val lists: StateFlow<List<ListEntity>> = _lists

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            ensureDefaultLists()
            refresh()
        }
    }

    private suspend fun ensureDefaultLists() {
        withContext(Dispatchers.IO) {
            val existing = repo.getLists()
            if (existing.isEmpty()) {
                repo.addList("Дома")
                repo.addList("Викендица")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val loaded = withContext(Dispatchers.IO) { repo.getLists() }
            _lists.value = loaded
            _loading.value = false
        }
    }

    fun addList(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.addList(clean) }
            refresh()
        }
    }
}
