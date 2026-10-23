package com.example.familyapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.sync.ListChangeStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListChangeStoreTest {
    @Test fun readStatePersistsAndLaterChangesReappearWithoutClearingOtherLists() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val other = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val maps = mapOf(1L to "a", 2L to "b")
            val store = ListChangeStore(db)
            store.update(mapOf("a" to 10L, "b" to 12L), null)
            assertEquals(setOf(1L,2L), store.unread(maps))
            store.markSeen("a")
            assertEquals(setOf(2L), ListChangeStore(db).unread(maps))
            assertTrue(ListChangeStore(other).unread(maps).isEmpty())
            store.update(mapOf("a" to 11L, "b" to 12L), null)
            assertEquals(setOf(1L,2L), store.unread(maps))
            store.update(mapOf("a" to 15L, "b" to 12L), "a")
            assertEquals(setOf(2L), store.unread(maps))
            // Returning to a closed list after newer remote work is unread again.
            store.update(mapOf("a" to 16L, "b" to 12L), null)
            assertEquals(setOf(1L,2L), store.unread(maps))
            // Successful access revocation removes the old watermark.
            store.update(mapOf("b" to 12L), null)
            assertEquals(setOf(2L), store.unread(maps))
        } finally { db.close(); other.close() }
    }
}
