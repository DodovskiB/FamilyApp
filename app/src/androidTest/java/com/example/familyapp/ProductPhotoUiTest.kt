package com.example.familyapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.ui.ShoppingItemsScreen
import com.example.familyapp.ui.ShoppingListScreen
import com.example.familyapp.viewmodel.MainMenuViewModel
import com.example.familyapp.viewmodel.MainMenuViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import repo.RoomFamilyRepository
import java.io.File

/** Supplies controlled camera/picker results without photographing the owner or browsing their gallery. */
@RunWith(AndroidJUnit4::class)
class ProductPhotoUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ViewModelStore()
    private lateinit var db: AppDatabase
    private lateinit var vm: MainMenuViewModel
    private lateinit var fixture: File
    private lateinit var registry: PhotoRegistry
    private var productId = 0L
    private var entryId = 0L
    private val listMode = mutableStateOf(false)

    private inner class PhotoRegistry : ActivityResultRegistry() {
        var cancelNext = false
        var cameraCalls = 0
        var galleryCalls = 0
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            val cancelled = cancelNext
            cancelNext = false
            when (contract) {
                is ActivityResultContracts.TakePicture -> {
                    cameraCalls++
                    val destination = input as Uri
                    assertEquals("${context.packageName}.photo-provider", destination.authority)
                    if (!cancelled) {
                        val image = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
                        context.contentResolver.openOutputStream(destination)!!.use { image.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                        image.recycle()
                    }
                    dispatchResult(requestCode, if (cancelled) Activity.RESULT_CANCELED else Activity.RESULT_OK, Intent())
                }
                is ActivityResultContracts.PickVisualMedia -> {
                    galleryCalls++
                    dispatchResult(requestCode, if (cancelled) Activity.RESULT_CANCELED else Activity.RESULT_OK,
                        Intent().setData(Uri.fromFile(fixture)))
                }
                else -> error("Unexpected contract")
            }
        }
    }

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = RoomFamilyRepository(db, ProductPhotoStore(context))
        productId = runBlocking { repo.addCatalogItem("Milk", "Dairy") }
        entryId = runBlocking { repo.addItem(repo.addList("Fixture list",0),"Shared entry",0) }
        fixture = File.createTempFile("gallery-fixture-", ".jpg", context.cacheDir)
        val image = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        fixture.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        image.recycle()
        registry = PhotoRegistry()
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        vm = ViewModelProvider(store, MainMenuViewModelFactory(repo))[MainMenuViewModel::class.java]
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MaterialTheme {
                    if (listMode.value) ShoppingListScreen(vm,onOpenMenu={})
                    else ShoppingItemsScreen(vm, onBack = {})
                }
            }
        }
        compose.waitUntil(5_000) { vm.catalogItems.value.isNotEmpty() }
    }

    @After
    fun cleanup() {
        val photo = runBlocking { db.catalogItemDao().getById(productId)?.photoFileName }
        val entryPhoto = runBlocking { db.itemDao().getById(entryId)?.photoFileName }
        compose.runOnIdle { store.clear() }
        ProductPhotoStore(context).delete(photo)
        ProductPhotoStore(context).delete(entryPhoto)
        db.close()
        fixture.delete()
    }

    private fun choose(action: String) {
        compose.onNodeWithText("Milk", substring = false).performTouchInput { longClick() }
        compose.onNodeWithText(action, substring = false).performClick()
    }

    @Test fun unlinkedSharedEntryActionsDoNotModifyPrivateCatalog() {
        runBlocking { db.itemDao().update(db.itemDao().getById(entryId)!!.copy(photoRemotePath="pending-download.jpg")) }
        compose.runOnIdle { listMode.value=true }
        compose.waitUntil(5_000) { vm.lists.value.isNotEmpty() }
        compose.onNodeWithText("Fixture list",substring=false).performClick()
        fun action(text:String) {
            compose.onNodeWithText("Shared entry",substring=false).performTouchInput { longClick() }
            compose.onNodeWithText(text,substring=false).performClick()
        }
        action("Remove photo")
        compose.waitUntil(5_000) { runBlocking { db.itemDao().getById(entryId)!!.photoRemotePath }==null && !vm.catalogSaving.value }
        action("Take photo")
        compose.waitUntil(5_000) { runBlocking { db.itemDao().getById(entryId)!!.photoFileName }!=null && !vm.catalogSaving.value }
        val cameraPhoto=runBlocking { db.itemDao().getById(entryId)!!.photoFileName!! }
        assertEquals(1,registry.cameraCalls)
        action("Choose from gallery")
        compose.waitUntil(5_000) { runBlocking { db.itemDao().getById(entryId)!!.photoFileName }.let { it!=null && it!=cameraPhoto } && !vm.catalogSaving.value }
        assertEquals(1,registry.galleryCalls)
        assertNull(runBlocking { db.catalogItemDao().getById(productId)!!.photoFileName })
        action("Remove photo")
        compose.waitUntil(5_000) { runBlocking { db.itemDao().getById(entryId)!!.photoFileName }==null && !vm.catalogSaving.value }
        assertNull(runBlocking { db.catalogItemDao().getById(productId)!!.photoFileName })
    }

    @Test
    fun cameraGalleryCancelAndRemovePhotoWorkFromLongPress() {
        choose("Take photo")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Photo: Milk").fetchSemanticsNodes().isNotEmpty()
        }
        val cameraPhoto = vm.catalogItems.value.single().photoFileName!!
        assertEquals(1, registry.cameraCalls)
        compose.runOnIdle { registry.cancelNext = true }
        choose("Take photo")
        compose.waitForIdle()
        assertEquals(cameraPhoto, vm.catalogItems.value.single().photoFileName)
        choose("Choose from gallery")
        compose.waitUntil(5_000) {
            vm.catalogItems.value.single().photoFileName.let { it != null && it != cameraPhoto }
        }
        val galleryPhoto = vm.catalogItems.value.single().photoFileName!!
        assertEquals(1, registry.galleryCalls)
        assertTrue(fixture.exists())
        assertFalse(ProductPhotoStore(context).file(cameraPhoto).exists())
        compose.runOnIdle { registry.cancelNext = true }
        choose("Choose from gallery")
        compose.waitForIdle()
        assertEquals(galleryPhoto, vm.catalogItems.value.single().photoFileName)
        choose("View photo")
        compose.onNodeWithText("Close", substring = false).performClick()
        choose("Remove photo")
        compose.waitUntil(5_000) { vm.catalogItems.value.single().photoFileName == null && !vm.catalogSaving.value }
        assertFalse(ProductPhotoStore(context).file(galleryPhoto).exists())
        assertTrue(fixture.exists())
    }
}
