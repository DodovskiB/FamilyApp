package com.example.familyapp

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.ui.FamilyAppRoot
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository
import java.util.Locale

/** Isolated fixture: no owner database or persisted device language is changed. */
@RunWith(AndroidJUnit4::class)
class LanguageUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private var db: AppDatabase? = null

    @After fun cleanup() {
        compose.runOnIdle { store.clear() }
        db?.close()
    }

    @Test fun macedonianScreensKeepUserNamesAndDirectLanguageChoice() {
        val original = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(original.resources.configuration).apply { setLocale(Locale.forLanguageTag("mk")) }
        val context = original.createConfigurationContext(config)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().also { db = it }
        val repo = RoomFamilyRepository(database)
        runBlocking { repo.addList("Home", 0); repo.addCatalogItem("Leb", "Bakery") }
        val vm = ViewModelProvider(store, MainMenuViewModelFactory(repo))[MainMenuViewModel::class.java]
        compose.setContent {
            val registryOwner = requireNotNull(LocalActivityResultRegistryOwner.current)
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalActivityResultRegistryOwner provides registryOwner) {
                MaterialTheme { FamilyAppRoot(vm) }
            }
        }
        compose.waitUntil(5_000) { vm.lists.value.size == 1 }
        compose.onNodeWithText("Нов список").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithContentDescription("Отвори мени").performClick()
        compose.onNodeWithText("Каталог на производи").assertIsDisplayed()
        compose.onNodeWithText("Јазик").assertIsDisplayed()
        compose.onNodeWithText("EN").assertIsNotSelected()
        compose.onNodeWithText("MK").assertIsSelected()
        compose.onNodeWithText("Каталог на производи").performClick()
        compose.onNodeWithText("Leb").assertIsDisplayed()
        compose.onNodeWithText("Bakery").assertIsDisplayed()
    }
}
