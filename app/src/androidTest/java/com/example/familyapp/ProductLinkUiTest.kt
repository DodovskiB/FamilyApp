package com.example.familyapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class ProductLinkUiTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private lateinit var db: AppDatabase
    private lateinit var vm: MainMenuViewModel
    private var opened: String? = null

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = RoomFamilyRepository(db)
        runBlocking { repo.addList("Home", 0); repo.addCatalogItem("Milk", "Dairy") }
        vm = ViewModelProvider(store, MainMenuViewModelFactory(repo))[MainMenuViewModel::class.java]
        val browser = object : UriHandler { override fun openUri(uri: String) { opened = uri } }
        compose.setContent { CompositionLocalProvider(LocalUriHandler provides browser) {
            MaterialTheme { FamilyAppRoot(vm) }
        } }
        compose.waitUntil(5_000) { vm.lists.value.isNotEmpty() }
    }

    @After
    fun cleanup() { compose.runOnIdle { store.clear() }; db.close() }

    private fun holdMilk() {
        compose.onNodeWithText("Milk", substring = false).performTouchInput { longClick() }
    }

    @Test
    fun linksCanBeValidatedEditedOpenedAndRemovedInCatalogAndList() {
        compose.onNodeWithText("New list name").assertDoesNotExist()
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithText("Product catalog", substring = false).performClick()
        compose.waitUntil(5_000) { vm.catalogItems.value.isNotEmpty() }
        holdMilk()
        compose.onNodeWithText("Add link", substring = false).performClick()
        compose.onNodeWithText("Product link").performTextInput("javascript:alert(1)")
        compose.onNodeWithText("Save", substring = false).assertIsNotEnabled()
        compose.onNodeWithText("Product link").performTextReplacement("www.amazon.com/dp/example")
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(5_000) { vm.catalogItems.value.single().linkUrl != null && !vm.catalogSaving.value }
        holdMilk()
        compose.onNodeWithText("Open link", substring = false).performClick()
        compose.runOnIdle { assertEquals("https://www.amazon.com/dp/example", opened) }
        holdMilk()
        compose.onNodeWithText("Edit link", substring = false).performClick()
        compose.onNodeWithText("Product link").performTextReplacement("https://www.ebay.com/itm/example")
        compose.onNodeWithText("Cancel", substring = false).performClick()
        compose.runOnIdle { assertEquals("https://www.amazon.com/dp/example", vm.catalogItems.value.single().linkUrl) }
        compose.onNodeWithText("Back", substring = false).performClick()
        compose.onNodeWithText("Home", substring = false).performClick()
        compose.onNodeWithText("Add product", substring = false).performClick()
        compose.onNodeWithText("Search products").performTextInput("Milk")
        compose.onNode(hasText("Milk", substring = false) and !hasSetTextAction()).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Increase quantity: Milk").fetchSemanticsNodes().isNotEmpty()
        }
        holdMilk()
        compose.onNodeWithText("Edit link", substring = false).performClick()
        compose.onNodeWithText("Product link").performTextReplacement("https://www.ebay.com/itm/example")
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(5_000) { vm.catalogItems.value.single().linkUrl == "https://www.ebay.com/itm/example" && !vm.catalogSaving.value }
        holdMilk()
        compose.onNodeWithText("Open link", substring = false).performClick()
        compose.runOnIdle { assertEquals("https://www.ebay.com/itm/example", opened) }
        holdMilk()
        compose.onNodeWithText("Edit link", substring = false).performClick()
        compose.onNodeWithText("Remove link", substring = false).performClick()
        compose.waitUntil(5_000) { vm.catalogItems.value.single().linkUrl == null && !vm.catalogSaving.value }
        holdMilk()
        compose.onNodeWithText("Open link", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Add link", substring = false).assertIsDisplayed()
    }
}
