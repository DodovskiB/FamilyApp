package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import repo.RoomFamilyRepository

class ChecklistViewModel(
    private val repo: RoomFamilyRepository,
    private val listId: Long
) : ViewModel() {

    val items = repo.observeItemsByList(listId)

    fun addItem(title: String) = viewModelScope.launch {
        repo.addItem(listId, title)
    }

    fun toggleChecked(itemId: Long, checked: Boolean) = viewModelScope.launch {
        repo.setChecked(itemId, checked)
    }

    fun increaseQty(itemId: Long, currentQty: Int) = viewModelScope.launch {
        repo.setQty(itemId, currentQty + 1)
    }

    fun decreaseQty(itemId: Long, currentQty: Int) = viewModelScope.launch {
        if (currentQty > 1) {
            repo.setQty(itemId, currentQty - 1)
        }
    }

    fun deleteItemSoft(itemId: Long) = viewModelScope.launch {
        repo.setActive(itemId, false)
    }
}
