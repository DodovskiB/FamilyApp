package com.example.familyapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.auth.AccountRepository
import com.example.familyapp.sync.SupabaseSyncTransport
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Opt-in live RPC proof on the signed-in phone; no real Room writes or UI login. */
@RunWith(AndroidJUnit4::class)
class PhysicalGroupCancelTest {
    @Test fun cancelsThreeInvitationsAndPreservesOtherGroups() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("physicalGroupCancel") == "true")
        val expectedOwner = requireNotNull(args.getString("testOwnerEmail"))
        val recipient = requireNotNull(args.getString("testRecipientEmail"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accounts = AccountRepository.getInstance(context)
        val authenticated = withTimeout(30_000) {
            requireNotNull(accounts.status).filterIsInstance<SessionStatus.Authenticated>().first()
        }
        val user = requireNotNull(authenticated.session.user)
        check(user.email == expectedOwner && recipient != expectedOwner && recipient.contains('@'))
        val remote = SupabaseSyncTransport(accounts,user.id)
        val ledger = File(context.filesDir,"disposable-group-cancel-fixture.json")
        check(!ledger.exists()) { "Previous fixture needs cleanup before rerunning" }
        fun id() = UUID.randomUUID().toString()
        val lists = List(3) { id() }
        val before = remote.snapshot()
        val existingInvitations = before.getValue("invitations").jsonArray.map {
            it.jsonObject.getValue("id").jsonPrimitive.content
        }.toSet()
        val existingMembers = before.getValue("members").jsonArray.toSet()
        ledger.writeText(buildJsonObject {
            put("owner",user.id); put("lists",buildJsonArray { lists.forEach { add(it) } })
        }.toString())
        try {
            remote.apply(buildJsonArray { lists.forEachIndexed { index,list -> add(buildJsonObject {
                put("operation_id",id()); put("entity","list"); put("id",list); put("action","insert")
                put("body",buildJsonObject { put("name","Disposable cancel proof ${index+1}"); put("sort_order",2003+index) })
            }) } })
            remote.inviteMany(lists,recipient)
            val rows = remote.snapshot().getValue("invitations").jsonArray.map { it.jsonObject }
                .filter { it.getValue("list_id").jsonPrimitive.content in lists }
            assertEquals(3,rows.size)
            val groups = rows.map { it.getValue("group_id").jsonPrimitive.contentOrNull }.toSet()
            assertEquals(1,groups.size); assertNotNull(groups.single())
            val invitation = rows.single { it.getValue("list_id").jsonPrimitive.content == lists.first() }
                .getValue("id").jsonPrimitive.content
            remote.remove(lists.first(),invitation=invitation)
            remote.remove(lists.first(),invitation=invitation) // retry is safe
            val after = remote.snapshot()
            val remaining = after.getValue("invitations").jsonArray.map { it.jsonObject }
            assertFalse(remaining.any { it.getValue("list_id").jsonPrimitive.content in lists })
            assertTrue(remaining.map { it.getValue("id").jsonPrimitive.content }.containsAll(existingInvitations))
            assertEquals(existingMembers,after.getValue("members").jsonArray.toSet())
        } finally {
            // Generated IDs only; ledger remains private if the network prevents cleanup.
            lists.forEach { list ->
                val pending = remote.snapshot().getValue("invitations").jsonArray.map { it.jsonObject }
                    .firstOrNull { it.getValue("list_id").jsonPrimitive.content == list }
                pending?.let { remote.remove(list,invitation=it.getValue("id").jsonPrimitive.content) }
            }
            remote.apply(buildJsonArray { lists.forEach { list -> add(buildJsonObject {
                put("operation_id",id()); put("entity","list"); put("id",list); put("action","delete")
                put("body",buildJsonObject {})
            }) } })
            val final = remote.snapshot()
            assertFalse(final.getValue("access").jsonArray.any { it.jsonObject.getValue("id").jsonPrimitive.content in lists })
            assertFalse(final.getValue("invitations").jsonArray.any { it.jsonObject.getValue("list_id").jsonPrimitive.content in lists })
            check(ledger.delete())
        }
    }
}
