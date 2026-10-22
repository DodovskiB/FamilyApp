package com.example.familyapp.ui

import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import com.example.familyapp.data.entity.ListEntity
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
                  onAction: (String, Long?, String, String?, String?) -> Unit,
                  ownedLists: List<ListEntity> = emptyList(), onInviteLists: (List<Long>, String) -> Unit = { _, _ -> }) {
    var email by rememberSaveable(list) { mutableStateOf("") }
    var removeMember by rememberSaveable(list) { mutableStateOf<String?>(null) }
    var cancelInvitation by rememberSaveable(list) { mutableStateOf<String?>(null) }
    val access = state.access[list]
    var selectedLists by rememberSaveable(list) { mutableStateOf(listOfNotNull(list)) }
    val available = ownedLists.filter { state.access[it.id]?.role == "owner" }
    val selected = selectedLists.filter { id -> available.any { it.id == id } }
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
                Text(stringResource(R.string.select_lists), style = MaterialTheme.typography.titleMedium)
                available.forEach { candidate ->
                    val checked = candidate.id in selected
                    Row(Modifier.fillMaxWidth().toggleable(value = checked, enabled = !state.busy, role = Role.Checkbox,
                        onValueChange = { selectedLists = if (it) selectedLists + candidate.id else selectedLists - candidate.id }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(localizedListName(candidate.name), Modifier.weight(1f))
                    }
                }
                Button(enabled = state.ready && !state.busy && email.isNotBlank() && selected.isNotEmpty(),
                    onClick = { onInviteLists(selected, email) }) { Text(stringResource(R.string.send_invitation)) }
                Text(stringResource(R.string.invite_info))
                ownedLists.firstOrNull { it.id == list }?.let {
                    HorizontalDivider()
                    Text(stringResource(R.string.members_for_list, localizedListName(it.name)), style = MaterialTheme.typography.titleMedium)
                }
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
            val incoming = state.invitations.filter { it.incoming }.groupBy { it.groupId ?: it.id }
            if (incoming.isEmpty()) Text(stringResource(R.string.no_invitations))
            incoming.forEach { (_, entries) ->
                val invitation = entries.first()
                val grouped = invitation.groupId != null
                val responseId = invitation.groupId ?: invitation.id
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        entries.forEach { Text(localizedListName(it.name), style = MaterialTheme.typography.titleMedium) }
                        Text(stringResource(R.string.invited_admin, invitation.email))
                        Row {
                            TextButton(enabled = !state.busy, onClick = { onAction(if (grouped) "accept_many" else "accept", null, "", responseId, null) }) { Text(stringResource(R.string.accept)) }
                            TextButton(enabled = !state.busy, onClick = { onAction(if (grouped) "decline_many" else "decline", null, "", responseId, null) }) { Text(stringResource(R.string.decline)) }
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
