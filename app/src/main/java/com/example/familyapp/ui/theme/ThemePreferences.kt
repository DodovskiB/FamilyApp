package com.example.familyapp.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;
    fun isDark(systemDark: Boolean) = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

class ThemePreferences(context: Context, name: String = "shopping_appearance") {
    private val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val mutableMode = MutableStateFlow(runCatching {
        ThemeMode.valueOf(preferences.getString("theme", ThemeMode.SYSTEM.name)!!)
    }.getOrDefault(ThemeMode.SYSTEM))
    val mode = mutableMode.asStateFlow()

    fun select(mode: ThemeMode) {
        preferences.edit().putString("theme", mode.name).apply()
        mutableMode.value = mode
    }
}
