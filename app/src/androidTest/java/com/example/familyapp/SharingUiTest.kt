package com.example.familyapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.sync.*
import com.example.familyapp.ui.SharingScreen
import com.example.familyapp.ui.ShoppingListScreen
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class SharingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ownerOnlyActionsStayInLongPressMenuAndAreAbsentForAdmin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            val id = runBlocking { RoomFamilyRepository(db).addList("Disposable UI list", 0) }
            val vm = ViewModelProvider(store, MainMenuViewModelFactory(RoomFamilyRepository(db)))[MainMenuViewModel::class.java]
            val state = mutableStateOf(SharingState(access = mapOf(id to ListAccess("list", "owner", "admin")), ready = true))
            compose.setContent { MaterialTheme { ShoppingListScreen(vm, {}, state.value) } }
            compose.waitUntil(5_000) { vm.lists.value.isNotEmpty() }
            compose.onNodeWithText("Delete").assertDoesNotExist()
            compose.onNodeWithText("Share / Manage members").assertDoesNotExist()
            compose.onNodeWithText("Disposable UI list").performTouchInput { longClick() }
            compose.onNodeWithText("Rename").assertIsDisplayed()
            compose.onNodeWithText("Delete").assertDoesNotExist()
            compose.onNodeWithText("Share / Manage members").assertDoesNotExist()
            compose.runOnIdle { state.value = state.value.copy(access = mapOf(id to ListAccess("list", "owner", "owner"))) }
            compose.onNodeWithText("Delete").assertIsDisplayed()
            compose.onNodeWithText("Share / Manage members").assertIsDisplayed()
        } finally { compose.runOnIdle { store.clear() }; db.close() }
    }

    @Test fun invitationsRequireExplicitAcceptOrDecline() {
        var action: String? = null
        var selected: String? = null
        val state = SharingState(invitations = listOf(ListInvitation("invitation", "list", "Disposable invitation", "recipient@sharing.invalid", true)))
        compose.setContent { MaterialTheme { SharingScreen(state, null, {}, { a, _, _, invitation, _ -> action = a; selected = invitation }) } }
        compose.onNodeWithText("Disposable invitation").assertIsDisplayed()
        compose.onNodeWithText("Accept").performClick()
        compose.runOnIdle { assertEquals("accept", action); assertEquals("invitation", selected) }
        compose.onNodeWithText("Decline").performClick()
        compose.runOnIdle { assertEquals("decline", action) }
    }
}
