package com.example.familyapp

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale

/** English assertions must not depend on the personal phone's saved app locale. */
fun ComposeContentTestRule.setEnglishContent(content: @Composable () -> Unit) {
    val base = InstrumentationRegistry.getInstrumentation().targetContext
    val config = Configuration(base.resources.configuration).apply { setLocale(Locale.ENGLISH) }
    val english = base.createConfigurationContext(config)
    setContent {
        val registry = requireNotNull(LocalActivityResultRegistryOwner.current)
        CompositionLocalProvider(LocalContext provides english,
            LocalConfiguration provides english.resources.configuration,
            LocalActivityResultRegistryOwner provides registry) { content() }
    }
}
