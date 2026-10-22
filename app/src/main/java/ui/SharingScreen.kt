package com.example.familyapp.ui

import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
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
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(if (list == null) stringResource(R.string.invitations) else stringResource(R.string.share_members), style = MaterialTheme.typography.headlineSmall)
        if (state.message != null) Text(localizedMessage(state.message))
        TextButton(enabled = !state.busy, onClick = { onAction("refresh", null, "", null, null) }) { Text(stringResource(R.string.refresh)) }
        if (list != null) {
            if (access == null) Text(stringResource(R.string.sharing_connect))
            else if (access.role == "admin") Text(stringResource(R.string.admin_info))
            else {
                Text(stringResource(R.string.owner_info))
                OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.google_email)) }, singleLine = true, enabled = !state.busy)
                Button(enabled = !state.busy && email.isNotBlank(), onClick = { onAction("invite", list, email, null, null) }) { Text(stringResource(R.string.invite)) }
                Text(stringResource(R.string.invite_info))
                Text(stringResource(R.string.admins), style = MaterialTheme.typography.titleMedium)
                val members = state.members.filter { it.listId == access.id }
                if (members.isEmpty()) Text(stringResource(R.string.no_members))
                members.forEach { member ->
                    Card(Modifier.fillMaxWidth().combinedClickable(enabled = !state.busy, onClick = {},
                        onLongClickLabel = stringResource(R.string.remove_access), onLongClick = { removeMember = member.id })) {
                        Text(member.email, Modifier.padding(16.dp))
                    }
                }
                Text(stringResource(R.string.pending_invitations), style = MaterialTheme.typography.titleMedium)
                state.invitations.filter { !it.incoming && it.listId == access.id }.forEach { invitation ->
                    Card(Modifier.fillMaxWidth().combinedClickable(enabled = !state.busy, onClick = {},
                        onLongClickLabel = stringResource(R.string.cancel_invitation), onLongClick = { cancelInvitation = invitation.id })) {
                        Text(invitation.email, Modifier.padding(16.dp))
                    }
                }
            }
        } else {
            val incoming = state.invitations.filter { it.incoming }
            if (incoming.isEmpty()) Text(stringResource(R.string.no_invitations))
            incoming.forEach { invitation ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(localizedListName(invitation.name), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.invited_admin, invitation.email))
                        Row {
                            TextButton(enabled = !state.busy, onClick = { onAction("accept", null, "", invitation.id, null) }) { Text(stringResource(R.string.accept)) }
                            TextButton(enabled = !state.busy, onClick = { onAction("decline", null, "", invitation.id, null) }) { Text(stringResource(R.string.decline)) }
                        }
                    }
                }
            }
        }
    }
    if (removeMember != null || cancelInvitation != null) {
        AlertDialog(onDismissRequest = { removeMember = null; cancelInvitation = null },
            title = { Text(if (removeMember != null) stringResource(R.string.remove_access_confirm) else stringResource(R.string.cancel_invitation_confirm)) },
            text = { Text(stringResource(R.string.remove_access_info)) },
            confirmButton = { TextButton(enabled = !state.busy, onClick = {
                onAction("remove", list, "", cancelInvitation, removeMember)
                removeMember = null; cancelInvitation = null
            }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { removeMember = null; cancelInvitation = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
