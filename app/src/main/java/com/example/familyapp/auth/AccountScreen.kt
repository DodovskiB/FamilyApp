package com.example.familyapp.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(state: AccountState, onSignIn: () -> Unit, onSignOut: () -> Unit,
                  onBack: (() -> Unit)? = null) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${stringResource(R.string.app_name)} account", style = MaterialTheme.typography.headlineMedium)
            if (state.loading) {
                CircularProgressIndicator()
            } else if (state.userId != null) {
                Text(state.name ?: "Signed in", style = MaterialTheme.typography.titleLarge)
                state.email?.let { Text(it) }
                Text("Signed in with Google. Your lists are currently saved on this phone. Sharing and synchronization are being added next.")
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
