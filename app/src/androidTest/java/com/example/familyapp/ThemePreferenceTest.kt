package com.example.familyapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.ui.theme.ThemeMode
import com.example.familyapp.ui.theme.ThemePreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ThemePreferenceTest {
    @Test fun systemDefaultAndExplicitChoicesSurviveNewPreferenceInstance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "isolated-theme-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            val choice = ThemePreferences(context, name)
            assertEquals(ThemeMode.SYSTEM, choice.mode.value)
            assertFalse(choice.mode.value.isDark(false)); assertTrue(choice.mode.value.isDark(true))
            choice.select(ThemeMode.DARK)
            assertTrue(ThemePreferences(context,name).mode.value.isDark(false))
            choice.select(ThemeMode.LIGHT)
            assertFalse(ThemePreferences(context,name).mode.value.isDark(true))
            choice.select(ThemeMode.SYSTEM)
            assertEquals(ThemeMode.SYSTEM, ThemePreferences(context,name).mode.value)
            prefs.edit().putString("theme", "unknown future value").commit()
            assertEquals(ThemeMode.SYSTEM, ThemePreferences(context,name).mode.value)
        } finally { prefs.edit().clear().commit() }
    }
}
