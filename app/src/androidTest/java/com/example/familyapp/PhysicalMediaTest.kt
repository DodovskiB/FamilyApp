package com.example.familyapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.auth.AccountRepository
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.sync.*
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Explicitly opted-in, human-assisted two-account check. No real Room DB writes. */
@RunWith(AndroidJUnit4::class)
class PhysicalMediaTest {
    @Test fun physicalTwoAccountMediaFixture() = runBlocking {
        val phase=InstrumentationRegistry.getArguments().getString("mediaPhysical")
        assumeTrue(phase in listOf("prepare","verify","cleanup"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val accounts=AccountRepository.getInstance(context)
        val session=withTimeout(30_000) { requireNotNull(accounts.status).filterIsInstance<SessionStatus.Authenticated>().first() }
        val user=requireNotNull(session.session.user)
        val ownerEmail=requireNotNull(InstrumentationRegistry.getArguments().getString("mediaOwnerEmail"))
        check(user.email==ownerEmail) { "Wrong test owner account" }
        val remote=SupabaseSyncTransport(accounts,user.id)
        val stateFile=File(context.filesDir,"disposable-physical-media-fixture.json")
        fun id()=UUID.randomUUID().toString()
        if(phase=="prepare") {
            check(!stateFile.exists()) { "Clean up existing disposable fixture first" }
            val list=id();val item=id()
            val bitmap=Bitmap.createBitmap(640,400,Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                drawColor(Color.rgb(25,125,80))
                drawText("Shared photo test",35f,210f,Paint().apply { color=Color.WHITE;textSize=48f;isAntiAlias=true })
            }
            val output=ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,85,output);bitmap.recycle()
            val bytes=output.toByteArray()
            val path="${user.id}/$list/${ProductPhotoStore.digest(bytes)}.jpg"
            stateFile.writeText(buildJsonObject { put("owner",user.id);put("list",list);put("item",item);put("path",path) }.toString())
            remote.apply(buildJsonArray {
                add(buildJsonObject { put("operation_id",id());put("entity","list");put("id",list);put("action","insert")
                    put("body",buildJsonObject { put("name","Media sharing test");put("sort_order",2002) }) })
                add(buildJsonObject { put("operation_id",id());put("entity","item");put("id",item);put("action","insert")
                    put("body",buildJsonObject { put("title","Shared photo test");put("category","Other");put("qty",1)
                        put("checked",false);put("active",true);put("list_id",list);put("catalog_id",JsonNull) }) })
            })
            remote.uploadPhoto(path,bytes)
            remote.applyMedia(MediaChange(id(),0,item,3,"https://example.com/shared-photo-test",null,null),
                buildJsonObject { put("owner_id",user.id);put("list_id",list) },path)
            val email=InstrumentationRegistry.getArguments().getString("mediaInviteEmail")
            require(email!=null && email.contains('@') && email!=ownerEmail) { "Explicit second test recipient required" }
            remote.invite(list,email)
        } else {
            val state=Json.parseToJsonElement(stateFile.readText()).jsonObject
            fun value(key:String)=state.getValue(key).jsonPrimitive.content
            require(value("owner")==user.id)
            val list=value("list");val item=value("item")
            if(phase=="verify") {
                val row=remote.mediaSnapshot().map { it.jsonObject }.single { it["id"]?.jsonPrimitive?.content==item }
                assertEquals("https://example.com/edited-by-honor",row.getValue("link_url").jsonPrimitive.content)
                assertEquals(JsonNull,row.getValue("photo_path"))
            } else {
                remote.apply(buildJsonArray { add(buildJsonObject {
                    put("operation_id",id());put("entity","list");put("id",list);put("action","delete");put("body",buildJsonObject {})
                }) })
                assertTrue(remote.obsoletePhotos().contains(value("path")))
                remote.deletePhoto(value("path"))
                assertFalse(remote.obsoletePhotos().contains(value("path")))
                assertFalse(remote.mediaSnapshot().any { it.jsonObject["id"]?.jsonPrimitive?.content==item })
                check(stateFile.delete())
            }
        }
    }
}
