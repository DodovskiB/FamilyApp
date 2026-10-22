package com.example.familyapp.sync

data class ListAccess(val id: String, val owner: String, val role: String)
data class ListInvitation(val id: String, val listId: String, val name: String, val email: String, val incoming: Boolean, val groupId: String? = null)
data class ListMember(val id: String, val listId: String, val email: String)
data class SharingState(
    val access: Map<Long, ListAccess> = emptyMap(),
    val invitations: List<ListInvitation> = emptyList(),
    val members: List<ListMember> = emptyList(),
    val busy: Boolean = false,
    val ready: Boolean = false,
    val message: String? = null
) {
    val incomingCount: Int get() = invitations.filter { it.incoming }.map { it.groupId ?: it.id }.distinct().size
}
