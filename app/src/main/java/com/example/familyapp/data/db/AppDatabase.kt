package com.example.familyapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.familyapp.data.dao.CatalogItemDao
import com.example.familyapp.data.entity.CatalogItemEntity
import com.example.familyapp.data.dao.ItemDao
import com.example.familyapp.data.dao.ListDao
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity

@Database(
    entities = [ListEntity::class, ItemEntity::class, CatalogItemEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao
    abstract fun catalogItemDao(): CatalogItemDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_items ADD COLUMN photoFileName TEXT")
                db.execSQL("UPDATE lists SET name = 'Home' WHERE trim(name) COLLATE NOCASE IN ('Doma', 'Дома', 'дома')")
                db.execSQL("UPDATE lists SET name = 'Weekend House' WHERE trim(name) COLLATE NOCASE IN ('Vikendica', 'Викендица', 'викендица')")
                db.execSQL("UPDATE lists SET name = 'Vacation' WHERE trim(name) COLLATE NOCASE IN ('Odmor', 'Одмор', 'одмор')")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN catalogItemId INTEGER")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS catalog_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, category TEXT NOT NULL)"
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "familyapp.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
