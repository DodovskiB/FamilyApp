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
import com.example.familyapp.ui.FamilyAppRoot
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
class FamilyHomeUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private lateinit var db: AppDatabase
    private lateinit var vm: MainMenuViewModel

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = RoomFamilyRepository(db)
        runBlocking {
            repo.addList("Doma", 0)
            repo.addList("Vikendica", 0)
            repo.addList("Odmor", 0)
            repo.addCatalogItem("Leb", "Bakery")
            repo.addCatalogItem("Lubenica", "Fruit")
            repo.addCatalogItem("Milk", "Dairy")
        }
        vm = ViewModelProvider(store, MainMenuViewModelFactory(repo))[MainMenuViewModel::class.java]
        compose.setContent { MaterialTheme { FamilyAppRoot(vm) } }
    }

    @After
    fun cleanup() {
        compose.runOnIdle { store.clear() }
        db.close()
    }

    @Test
    fun homeShowsListsAndProductSearchSuggestsAndCreates() {
        compose.waitUntil(5_000) { vm.lists.value.size == 3 }
        compose.onNodeWithText("Doma", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Vikendica", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Odmor", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Shopping Items", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Doma", substring = false).performClick()
        compose.onNodeWithText("Add product", substring = false).performClick()
        compose.onNodeWithText("Search products").performTextInput("l")
        compose.onNodeWithText("Leb", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Lubenica", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Milk", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Lubenica", substring = false).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Increase quantity: Lubenica").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add product", substring = false).performClick()
        compose.onNodeWithText("Search products").performTextInput("Limon")
        compose.onNodeWithText("No matching products. You can create one below.").assertIsDisplayed()
        compose.onNodeWithText("New product", substring = false).performClick()
        compose.onNode(hasSetTextAction() and hasText("Limon")).assertIsDisplayed()
        compose.onNodeWithText("Save and add").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Increase quantity: Limon").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Back to lists").performClick()
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithText("Item list", substring = false).performClick()
        compose.onNodeWithText("Shopping Items", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Limon", substring = false).assertIsDisplayed()
        compose.onNodeWithText("Back", substring = false).performClick()
        compose.onNodeWithText("Doma", substring = false).assertIsDisplayed()
    }
}
