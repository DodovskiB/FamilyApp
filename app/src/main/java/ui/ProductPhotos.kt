package com.example.familyapp.ui

import androidx.compose.ui.res.stringResource
import com.example.familyapp.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.net.Uri
import com.example.familyapp.data.entity.CatalogItemEntity
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.viewmodel.MainMenuViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductPhotoActions(val camera: (Long) -> Unit, val gallery: (Long) -> Unit)

@Composable
fun rememberProductPhotoActions(viewModel: MainMenuViewModel): ProductPhotoActions {
    val context = LocalContext.current
    val store = remember(context) { ProductPhotoStore(context) }
    var galleryProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cameraProductId by rememberSaveable { mutableStateOf<Long?>(null) }
    var captureName by rememberSaveable { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = galleryProductId
        galleryProductId = null
        if (uri != null && id != null) viewModel.attachCatalogPhoto(id, uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val id = cameraProductId
        val name = captureName
        cameraProductId = null
        captureName = null
        val file = name?.let { store.cameraFile(it) }
        if (success && id != null && file != null) {
            viewModel.attachCatalogPhoto(id, Uri.fromFile(file)) { file.delete() }
        } else file?.delete()
    }
    return ProductPhotoActions(
        camera = { id ->
            viewModel.clearCatalogError()
            try {
                val file = store.newCameraFile()
                cameraProductId = id
                captureName = file.name
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.photo-provider", file)
                camera.launch(uri)
            } catch (_: Exception) {
                captureName?.let { store.cameraFile(it).delete() }
                captureName = null
                cameraProductId = null
                viewModel.photoLaunchFailed()
            }
        },
        gallery = { id ->
            viewModel.clearCatalogError()
            galleryProductId = id
            try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch (_: Exception) {
                galleryProductId = null
                viewModel.photoLaunchFailed()
            }
        }
    )
}

@Composable
fun ProductPhoto(fileName: String?, title: String, modifier: Modifier = Modifier, large: Boolean = false) {
    if (fileName == null) return
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(initialValue = null, fileName, large) {
        value = withContext(Dispatchers.IO) {
            runCatching { ProductPhotoStore(context).thumbnail(fileName, if (large) 1200 else 192)?.asImageBitmap() }.getOrNull()
        }
    }
    bitmap?.let {
        Image(it, contentDescription = stringResource(R.string.photo_description, title),
            modifier = modifier.size(if (large) 280.dp else 56.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = if (large) ContentScale.Fit else ContentScale.Crop)
    }
}

@Composable
fun PhotoMenuOptions(product: CatalogItemEntity, actions: ProductPhotoActions, enabled: Boolean,
                     onClose: () -> Unit, onView: () -> Unit, onRemove: () -> Unit) {
    TextButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
        onClose(); actions.camera(product.id)
    }) { Text(stringResource(R.string.take_photo)) }
    TextButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
        onClose(); actions.gallery(product.id)
    }) { Text(stringResource(R.string.choose_gallery)) }
    if (product.photoFileName != null) {
        TextButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
            onClose(); onView()
        }) { Text(stringResource(R.string.view_photo)) }
        TextButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
            onClose(); onRemove()
        }) { Text(stringResource(R.string.remove_photo)) }
    }
}

@Composable
fun ProductPhotoDialog(product: CatalogItemEntity, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(product.title) },
        text = { ProductPhoto(product.photoFileName, product.title, large = true) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}
