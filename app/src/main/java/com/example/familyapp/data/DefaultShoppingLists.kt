package com.example.familyapp.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DefaultShoppingLists {
    private const val INITIALIZED = "initial_lists_v1"

    suspend fun initialize(db: AppDatabase, preferences: SharedPreferences) = withContext(Dispatchers.IO) {
        if (preferences.getBoolean(INITIALIZED, false)) return@withContext
        db.withTransaction {
            if (db.listDao().getAll().isEmpty()) {
                listOf("Home", "Weekend House", "Vacation").forEachIndexed { index, name ->
                    db.listDao().insert(ListEntity(name = name, sortOrder = index))
                }
            }
        }
        check(preferences.edit().putBoolean(INITIALIZED, true).commit())
    }
}
