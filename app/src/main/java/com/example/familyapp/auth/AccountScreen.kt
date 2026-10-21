package com.example.familyapp.auth

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
            Text("${stringResource(R.string.app_name)} account", style = MaterialTheme.typography.headlineMedium)
            if (state.loading) {
                CircularProgressIndicator()
            } else if (state.userId != null) {
                Text(state.name ?: "Signed in", style = MaterialTheme.typography.titleLarge)
                state.email?.let { Text(it) }
                Text("Signed in with Google. Your shopping lists are connected to your account.")
                syncStatus?.let {
                    Text(it.message)
                    if (it.pending > 0) Text("${it.pending} changes waiting to sync")
                    if (it.needsImportChoice && onImportChoice != null) {
                        Text("Combine matching lists to keep every product entry, quantity and purchased status. Products are not deduplicated. Or keep this phone's lists separate.")
                        Button(onClick = { onImportChoice(true) }) { Text("Combine matching lists") }
                        TextButton(onClick = { onImportChoice(false) }) { Text("Keep lists separate") }
                    }
                }
                Text("Lists sync while the app is open. Product photos are currently saved on this phone.")
                Button(onClick = onSignOut, enabled = !state.busy) { Text("Sign out") }
            } else {
                Text("Sign in with your Google account to connect ${stringResource(R.string.app_name)} to your profile.")
                if (!state.requiresSignIn) {
                    Text("The existing lists on this phone will be associated with the first account you sign in with.")
                }
                if (state.configured) {
                    Button(onClick = onSignIn, enabled = !state.busy) { Text("Continue with Google") }
                } else {
                    Text("Account setup is in progress. You can keep using your local shopping lists.")
                }
            }
            if (state.busy) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            onBack?.let { TextButton(onClick = it) { Text("Back to lists") } }
        }
    }
}
