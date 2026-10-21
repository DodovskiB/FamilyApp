package com.example.familyapp.auth

import com.example.familyapp.ui.localizedMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import com.example.familyapp.sync.SyncStatus
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(state: AccountState, onSignIn: () -> Unit, onSignOut: () -> Unit,
                  syncStatus: SyncStatus? = null,
                  onImportChoice: ((Boolean) -> Unit)? = null,
                  onBack: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.account_title, stringResource(R.string.app_name)), style = MaterialTheme.typography.headlineMedium)
            if (state.loading) {
                CircularProgressIndicator()
            } else if (state.userId != null) {
                Text(state.name ?: stringResource(R.string.signed_in), style = MaterialTheme.typography.titleLarge)
                state.email?.let { Text(it) }
                Text(stringResource(R.string.signed_in_info))
                syncStatus?.let {
                    Text(localizedMessage(it.message))
                    if (it.pending > 0) Text(stringResource(R.string.pending_changes, it.pending))
                    if (it.needsImportChoice && onImportChoice != null) {
                        Text(stringResource(R.string.merge_info))
                        Button(onClick = { onImportChoice(true) }) { Text(stringResource(R.string.combine_lists)) }
                        TextButton(onClick = { onImportChoice(false) }) { Text(stringResource(R.string.keep_separate)) }
                    }
                }
                Text(stringResource(R.string.sync_info))
                Button(onClick = onSignOut, enabled = !state.busy) { Text(stringResource(R.string.sign_out)) }
            } else {
                Text(stringResource(R.string.sign_in_info, stringResource(R.string.app_name)))
                if (!state.requiresSignIn) {
                    Text(stringResource(R.string.first_sign_in))
                }
                if (state.configured) {
                    Button(onClick = onSignIn, enabled = !state.busy) { Text(stringResource(R.string.continue_google)) }
                } else {
                    Text(stringResource(R.string.account_setup))
                }
            }
            if (state.busy) CircularProgressIndicator()
            state.error?.let { Text(localizedMessage(it), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            onBack?.let { TextButton(onClick = it) { Text(stringResource(R.string.back_lists)) } }
        }
    }
}
