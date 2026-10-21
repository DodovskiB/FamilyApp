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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class PurchasedUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun sectionsRepeatConfirmationAndClearConfirmationKeepCatalogAndOutstanding() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val owner = ViewModelStore()
        try {
            val repo = RoomFamilyRepository(db)
            val (home, milk) = runBlocking {
                val home = repo.addList("Fixture home", 0)
                val milk = repo.addCatalogItem("Milk", "Dairy")
                val bread = repo.addCatalogItem("Bread", "Bakery")
                val entry = repo.addCatalogItemToList(home, milk)
                repo.setQty(entry, 5); repo.toggleChecked(entry, true)
                repo.addCatalogItemToList(home, bread)
                home to milk
            }
            val vm = ViewModelProvider(owner, MainMenuViewModelFactory(repo))[MainMenuViewModel::class.java]
            compose.setContent { MaterialTheme { FamilyAppRoot(vm) } }
            compose.waitUntil(5_000) { vm.lists.value.isNotEmpty() }
            compose.onNodeWithText("New list").assertIsDisplayed()
            compose.onNodeWithText("Fixture home").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Milk").fetchSemanticsNodes().isNotEmpty() }
            val purchasedTop = compose.onNodeWithText("Purchased").fetchSemanticsNode().boundsInRoot.top
            assertTrue(compose.onNodeWithText("Bread").fetchSemanticsNode().boundsInRoot.top < purchasedTop)
            assertTrue(compose.onNodeWithText("Milk").fetchSemanticsNode().boundsInRoot.top > purchasedTop)
            fun chooseMilk() {
                compose.onNodeWithText("Add product").performClick()
                compose.onNodeWithText("Search products").performTextInput("Milk")
                compose.onNode(hasText("Milk") and !hasSetTextAction() and hasAnyAncestor(isDialog())).performClick()
                compose.waitUntil(5_000) { vm.repeatProductId.value == milk }
            }
            chooseMilk()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(5, runBlocking { repo.getItems(home, 0).single { it.catalogItemId == milk }.qty })
            compose.onNodeWithText("Cancel").performClick()
            chooseMilk()
            compose.onNodeWithText("New quantity").performTextReplacement("2")
            compose.onNodeWithText("Buy again", substring = false).performClick()
            compose.waitUntil(5_000) { !vm.listSaving.value && vm.repeatProductId.value == null }
            compose.onNodeWithContentDescription("Purchased: Milk").assertIsOff().performClick()
            compose.waitUntil(5_000) { !vm.listSaving.value }
            compose.onNodeWithText("Back to lists").performClick()
            compose.onNodeWithText("Fixture home").performTouchInput { longClick() }
            compose.onNodeWithText("Clear purchased").performClick()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(2, runBlocking { repo.getItems(home, 0).size })
            compose.onNodeWithText("Fixture home").performTouchInput { longClick() }
            compose.onNodeWithText("Clear purchased").performClick()
            compose.onNodeWithText("Clear purchased", substring = false).performClick()
            compose.waitUntil(5_000) { !vm.listSaving.value }
            assertEquals(listOf("Bread"), runBlocking { repo.getItems(home, 0).map { it.title } })
            assertEquals(2, vm.catalogItems.value.size)
            compose.onNodeWithContentDescription("Open menu").performClick()
            // Only the underlying home button exists; no second drawer destination.
            compose.onAllNodesWithText("New list").assertCountEquals(1)
        } finally { compose.runOnIdle { owner.clear() }; db.close() }
    }
}
