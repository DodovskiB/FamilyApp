package com.example.familyapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.data.entity.CatalogItemEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import repo.FamilyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainMenuViewModel(
    private val repository: FamilyRepository
) : ViewModel() {

    val catalogItems: StateFlow<List<CatalogItemEntity>> =
        repository.observeCatalogItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _catalogSaving = MutableStateFlow(false)
    val catalogSaving = _catalogSaving.asStateFlow()

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError = _catalogError.asStateFlow()

    fun clearCatalogError() {
        _catalogError.value = null
    }

    fun photoLaunchFailed() {
        _catalogError.value = "Could not open the camera or gallery. Please try again."
    }

    fun attachCatalogPhoto(id: Long, uri: Uri, onFinished: () -> Unit = {}) {
        if (_catalogSaving.value) { onFinished(); return }
        changeCatalog({}) {
            try { repository.attachCatalogPhoto(id, uri) }
            finally { onFinished() }
        }
    }

    fun removeCatalogPhoto(id: Long) = changeCatalog({}) {
        repository.removeCatalogPhoto(id)
    }

    fun saveCatalogItem(
        item: CatalogItemEntity?,
        title: String,
        category: String,
        onSaved: () -> Unit
    ) = changeCatalog(onSaved) {
        if (item == null) repository.addCatalogItem(title, category)
        else repository.updateCatalogItem(item.copy(title = title, category = category))
    }

    fun deleteCatalogItem(id: Long, onDeleted: () -> Unit) = changeCatalog(onDeleted) {
        repository.deleteCatalogItem(id)
    }

    private fun changeCatalog(onSuccess: () -> Unit, action: suspend () -> Unit) {
        if (_catalogSaving.value) return
        _catalogSaving.value = true
        _catalogError.value = null
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _catalogError.value = "Could not save the change. Please try again."
            } finally {
                _catalogSaving.value = false
            }
        }
    }

    val lists: StateFlow<List<ListEntity>> =
        repository.observeLists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _listSaving = MutableStateFlow(false)
    val listSaving = _listSaving.asStateFlow()
    private val _listError = MutableStateFlow<String?>(null)
    val listError = _listError.asStateFlow()

    fun clearListError() { _listError.value = null }

    private fun changeList(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (_listSaving.value) return
        _listSaving.value = true
        _listError.value = null
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _listError.value = "Could not save the change. Please try again."
            } finally {
                _listSaving.value = false
            }
        }
    }

    fun listItems(listId: Long) = repository.observeItems(listId, kind = 0)

    fun addProductToList(listId: Long, productId: Long, onSuccess: () -> Unit) = changeList(onSuccess) {
        repository.addCatalogItemToList(listId, productId)
    }

    fun createProductForList(listId: Long, title: String, category: String, onSuccess: () -> Unit) = changeList(onSuccess) {
        repository.createProductForList(listId, title, category)
    }

    fun changeQuantity(itemId: Long, delta: Int) = changeList {
        repository.changeQty(itemId, delta)
    }

    fun checkItem(itemId: Long, checked: Boolean) = changeList {
        repository.toggleChecked(itemId, checked)
    }

    fun removeListItem(itemId: Long, onSuccess: () -> Unit) = changeList(onSuccess) {
        repository.deleteItemSoft(itemId)
    }

    fun addList(name: String, onSuccess: () -> Unit = {}) = changeList(onSuccess) {
        repository.addList(name)
    }

    fun renameList(listId: Long, newName: String, onSuccess: () -> Unit = {}) = changeList(onSuccess) {
        repository.renameList(listId, newName)
    }

    fun deleteList(listId: Long, onSuccess: () -> Unit = {}) = changeList(onSuccess) {
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
