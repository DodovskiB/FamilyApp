package com.example.familyapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity

@Database(
    entities = [ListEntity::class, ItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "familyapp.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
