package com.example.familyapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.ui.ShoppingListScreen
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class ShoppingListUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private lateinit var db: AppDatabase
    private lateinit var vm: MainMenuViewModel

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        runBlocking { RoomFamilyRepository(db).addCatalogItem("Milk", "Dairy") }
        vm = ViewModelProvider(store, MainMenuViewModelFactory(RoomFamilyRepository(db)))[MainMenuViewModel::class.java]
        compose.setContent { MaterialTheme { ShoppingListScreen(vm, onBack = {}) } }
    }

    @After
    fun cleanup() {
        compose.runOnIdle { store.clear() }
        db.close()
    }

    @Test
    fun userCanCreateRenameShopAndCancelRemovals() {
        compose.onNodeWithText("New list name").performTextInput("Home")
        compose.onNodeWithText("Add", substring = false).performClick()
        compose.waitUntil(5_000) { vm.lists.value.size == 1 }
        compose.onNodeWithText("Rename").assertDoesNotExist()
        compose.onNodeWithText("Delete", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Home", substring = false).performTouchInput { longClick() }
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("List name", substring = false).performTextReplacement("Holiday")
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(5_000) { vm.lists.value.singleOrNull()?.name == "Holiday" }
        compose.onNodeWithText("Holiday", substring = false).performClick()
        compose.onNodeWithText("Add product", substring = false).performClick()
        compose.onNodeWithText("Milk", substring = false).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Increase quantity: Milk").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Increase quantity: Milk").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("2", substring = false).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("2", substring = false).assertIsDisplayed()
        compose.onNodeWithContentDescription("Purchased: Milk").performClick()
        compose.waitUntil(5_000) { !vm.listSaving.value }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Purchased: Milk").assertIsOn()
        compose.onNodeWithText("Remove", substring = false).performClick()
        compose.onNodeWithText("Remove product?").assertIsDisplayed()
        compose.onNodeWithText("Cancel", substring = false).performClick()
        compose.onNodeWithText("Milk", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Back to lists").performClick()
        compose.onNodeWithText("Holiday", substring = false).performTouchInput { longClick() }
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.onNodeWithText("Delete list?").assertIsDisplayed()
        compose.onNodeWithText("Cancel", substring = false).performClick()
        compose.onNodeWithText("Holiday", substring = false).assertIsDisplayed()
    }
}
