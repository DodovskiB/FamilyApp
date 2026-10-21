package com.example.familyapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import androidx.compose.ui.unit.dp
import com.example.familyapp.viewmodel.MainMenuViewModel

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onShoppingList: () -> Unit,
    onShoppingItems: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.app_name))

        Button(onClick = onShoppingList) {
            Text(stringResource(R.string.shopping_list))
        }

        Button(onClick = onShoppingItems) {
            Text(stringResource(R.string.shopping_items))
        }
    }
}
