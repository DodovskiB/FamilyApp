package com.example.familyapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.familyapp.R

/** Translate standard display names only; stored/shared names and custom names stay intact. */
@Composable
fun localizedListName(name: String): String = when (name) {
    "Home" -> stringResource(R.string.default_list_home)
    "Weekend House" -> stringResource(R.string.default_list_weekend)
    "Vacation" -> stringResource(R.string.default_list_vacation)
    else -> name
}
