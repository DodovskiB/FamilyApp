package com.example.familyapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.familyapp.repo.FamilyRepository

class MainMenuViewModelFactory(
    private val repository: FamilyRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainMenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
