package com.example.familyapp.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.familyapp.R

@Composable
fun ThemeSelector(mode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    fun label(value: ThemeMode) = when (value) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
    NavigationDrawerItem(selected = false, onClick = { open = true },
        label = { Text(stringResource(R.string.theme)) },
        badge = { Text(stringResource(label(mode))) })
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.theme)) },
        text = { Column {
            ThemeMode.entries.forEach { choice ->
                Row(Modifier.fillMaxWidth().selectable(selected = mode == choice, role = Role.RadioButton,
                    onClick = { onChange(choice); open = false }).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == choice, onClick = null)
                    Text(stringResource(label(choice)))
                }
            }
        } },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } })
}
