package com.example.familyapp

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.sync.*
import com.example.familyapp.ui.*
import com.example.familyapp.ui.theme.*
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository

@RunWith(AndroidJUnit4::class)
class AppearanceChangesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun groupedCancelConfirmationListsEveryAffectedList() {
        var invitation: String? = null
        val state = SharingState(ready = true, access = mapOf(1L to ListAccess("one","owner","owner")), invitations = listOf(
            ListInvitation("a","one","First","fixture@batch.invalid",false,"group"),
            ListInvitation("b","two","Second","fixture@batch.invalid",false,"group"),
            ListInvitation("c","three","Third","fixture@batch.invalid",false,"group")))
        compose.setEnglishContent { MaterialTheme { SharingScreen(state,1L,{}, { action,_,_,id,_ ->
            assertEquals("remove",action);invitation=id
        }) } }
        compose.onNodeWithText("fixture@batch.invalid").performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithText("First").assertIsDisplayed()
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.onNodeWithText("Third").assertIsDisplayed()
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals("a", invitation) }
    }

    @Test fun openingListClearsBadgeAndReportsVisibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            val id = runBlocking { RoomFamilyRepository(db).addList("Unread fixture",0) }
            val vm = ViewModelProvider(store,MainMenuViewModelFactory(RoomFamilyRepository(db)))[MainMenuViewModel::class.java]
            var unread by mutableStateOf(setOf(id))
            var visible: Long? = null
            compose.setEnglishContent { MaterialTheme { ShoppingListScreen(vm,{},unreadLists=unread,
                onVisibleList={ visible=it; if(it != null) unread=emptySet() }) } }
            compose.waitUntil(5000) { vm.lists.value.isNotEmpty() }
            compose.onNodeWithContentDescription("New changes in this list",useUnmergedTree=true).assertExists()
            compose.onNodeWithText("Unread fixture").performClick()
            compose.runOnIdle { assertEquals(id,visible) }
            compose.onNodeWithText("Back to lists").performClick()
            compose.onNodeWithContentDescription("New changes in this list",useUnmergedTree=true).assertDoesNotExist()
            compose.runOnIdle { assertNull(visible) }
        } finally { compose.runOnIdle { store.clear() };db.close() }
    }

    @Test fun themeChoiceChangesActualPaletteAndSystemChoiceFollowsSystem() {
        var mode by mutableStateOf(ThemeMode.SYSTEM)
        var systemDark by mutableStateOf(false)
        var brightness = 0f
        compose.setEnglishContent { FamilyAppTheme(darkTheme=mode.isDark(systemDark),dynamicColor=false) {
            brightness=MaterialTheme.colorScheme.background.luminance()
            Column { ThemeSelector(mode) { mode=it } }
        } }
        compose.runOnIdle { assertTrue(brightness>0.5f) }
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Dark").performClick()
        compose.runOnIdle { assertEquals(ThemeMode.DARK,mode);assertTrue(brightness<0.5f) }
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Light").performClick()
        compose.runOnIdle { assertTrue(brightness>0.5f);systemDark=true }
        compose.runOnIdle { assertTrue(brightness>0.5f) }
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("System").performClick()
        compose.runOnIdle { assertTrue(brightness<0.5f) }
    }
}
