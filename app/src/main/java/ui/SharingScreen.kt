package com.example.familyapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.familyapp.sync.SharingState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharingScreen(state: SharingState, list: Long?, onBack: () -> Unit,
                  onAction: (String, Long?, String, String?, String?) -> Unit) {
    var email by rememberSaveable(list) { mutableStateOf("") }
    var removeMember by rememberSaveable(list) { mutableStateOf<String?>(null) }
    var cancelInvitation by rememberSaveable(list) { mutableStateOf<String?>(null) }
    val access = state.access[list]
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(if (list == null) "Invitations" else "Share / Manage members", style = MaterialTheme.typography.headlineSmall)
        if (state.message != null) Text(state.message)
        TextButton(enabled = !state.busy, onClick = { onAction("refresh", null, "", null, null) }) { Text("Refresh") }
        if (list != null) {
            if (access == null) Text("Connect to the internet and let this list sync before sharing it.")
            else if (access.role == "admin") Text("You are an Admin. You can edit this list and its products. The Owner manages access.")
            else {
                Text("You are the Owner. Invited people can edit this list as Admins. Your other lists and personal catalog stay private.")
                OutlinedTextField(email, { email = it }, label = { Text("Google account email") }, singleLine = true, enabled = !state.busy)
                Button(enabled = !state.busy && email.isNotBlank(), onClick = { onAction("invite", list, email, null, null) }) { Text("Invite") }
                Text("The invitation appears inside their app after Google sign-in. It expires after 7 days. No email is sent.")
                Text("Admins", style = MaterialTheme.typography.titleMedium)
                val members = state.members.filter { it.listId == access.id }
                if (members.isEmpty()) Text("No other members yet.")
                members.forEach { member ->
                    Card(Modifier.fillMaxWidth().combinedClickable(enabled = !state.busy, onClick = {},
                        onLongClickLabel = "Remove access", onLongClick = { removeMember = member.id })) {
                        Text(member.email, Modifier.padding(16.dp))
                    }
                }
                Text("Pending invitations", style = MaterialTheme.typography.titleMedium)
                state.invitations.filter { !it.incoming && it.listId == access.id }.forEach { invitation ->
                    Card(Modifier.fillMaxWidth().combinedClickable(enabled = !state.busy, onClick = {},
                        onLongClickLabel = "Cancel invitation", onLongClick = { cancelInvitation = invitation.id })) {
                        Text(invitation.email, Modifier.padding(16.dp))
                    }
                }
            }
        } else {
            val incoming = state.invitations.filter { it.incoming }
            if (incoming.isEmpty()) Text("No pending invitations for this Google account.")
            incoming.forEach { invitation ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(invitation.name, style = MaterialTheme.typography.titleMedium)
                        Text("Invited as Admin: ${invitation.email}")
                        Row {
                            TextButton(enabled = !state.busy, onClick = { onAction("accept", null, "", invitation.id, null) }) { Text("Accept") }
                            TextButton(enabled = !state.busy, onClick = { onAction("decline", null, "", invitation.id, null) }) { Text("Decline") }
                        }
                    }
                }
            }
        }
    }
    if (removeMember != null || cancelInvitation != null) {
        AlertDialog(onDismissRequest = { removeMember = null; cancelInvitation = null },
            title = { Text(if (removeMember != null) "Remove access?" else "Cancel invitation?") },
            text = { Text("This person will no longer be able to access this list. Other lists are kept.") },
            confirmButton = { TextButton(enabled = !state.busy, onClick = {
                onAction("remove", list, "", cancelInvitation, removeMember)
                removeMember = null; cancelInvitation = null
            }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { removeMember = null; cancelInvitation = null }) { Text("Cancel") } })
    }
}
