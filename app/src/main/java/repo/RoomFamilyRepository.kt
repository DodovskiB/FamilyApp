package repo

import android.net.Uri
import androidx.room.withTransaction
import com.example.familyapp.data.db.AppDatabase
import com.example.familyapp.data.entity.CatalogItemEntity
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.familyapp.data.photo.ProductPhotoStore
import com.example.familyapp.data.ProductLinks

class RoomFamilyRepository(private val db: AppDatabase, private val photos: ProductPhotoStore? = null) : FamilyRepository {
    private val listDao = db.listDao()
    private val itemDao = db.itemDao()
    private val catalogItemDao = db.catalogItemDao()

    override fun observeCatalogItems(): Flow<List<CatalogItemEntity>> = catalogItemDao.observeAll()

    override suspend fun setCatalogLink(id: Long, url: String) = db.withTransaction {
        val normalized = ProductLinks.normalize(url)
        requireNotNull(catalogItemDao.getById(id))
        catalogItemDao.setLink(id, normalized)
    }

    override suspend fun addCatalogItem(title: String, category: String): Long {
        require(title.isNotBlank()) { "Product name is required" }
        return catalogItemDao.insert(
            CatalogItemEntity(title = title.trim(), category = category.trim().ifBlank { "Other" })
        )
    }

    override suspend fun updateCatalogItem(item: CatalogItemEntity) {
        require(item.title.isNotBlank()) { "Product name is required" }
        catalogItemDao.updateDetails(item.id, item.title.trim(), item.category.trim().ifBlank { "Other" })
    }

    override suspend fun deleteCatalogItem(id: Long) {
        val oldPhoto = db.withTransaction {
            val old = catalogItemDao.getById(id)?.photoFileName
            itemDao.clearCatalogReference(id)
            catalogItemDao.deleteById(id)
            old
        }
        withContext(Dispatchers.IO) { photos?.delete(oldPhoto) }
    }

    override suspend fun attachCatalogPhoto(id: Long, uri: Uri) = withContext(Dispatchers.IO) {
        val store = checkNotNull(photos)
        val imported = store.importPhoto(uri)
        try {
            val oldPhoto = db.withTransaction {
                val product = requireNotNull(catalogItemDao.getById(id)) { "Product no longer exists" }
                catalogItemDao.setPhoto(id, imported)
                product.photoFileName
            }
            store.delete(oldPhoto)
        } catch (error: Exception) {
            store.delete(imported)
            throw error
        }
    }

    override suspend fun removeCatalogPhoto(id: Long) {
        val oldPhoto = db.withTransaction {
            val product = requireNotNull(catalogItemDao.getById(id))
            catalogItemDao.setPhoto(id, null)
            product.photoFileName
        }
        withContext(Dispatchers.IO) { photos?.delete(oldPhoto) }
    }

    override suspend fun addCatalogItemToList(listId: Long, catalogItemId: Long): Long =
        db.withTransaction {
            requireNotNull(listDao.getById(listId)) { "List no longer exists" }
            val product = requireNotNull(catalogItemDao.getById(catalogItemId)) { "Product no longer exists" }
            val existing = itemDao.findActiveCatalogItem(listId, catalogItemId)
            if (existing != null) {
                itemDao.changeQty(existing.id, 1)
                existing.id
            } else {
                itemDao.insert(
                    ItemEntity(
                        listId = listId, title = product.title, kind = 0,
                        category = product.category, qty = 1, checked = false,
                        isActive = true, catalogItemId = product.id
                    )
                )
            }
        }

    override suspend fun createProductForList(listId: Long, title: String, category: String): Long =
        db.withTransaction {
            requireNotNull(listDao.getById(listId)) { "List no longer exists" }
            val productId = addCatalogItem(title, category)
            addCatalogItemToList(listId, productId)
        }

    override fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()
    override suspend fun getLists(): List<ListEntity> = listDao.getAll()

    override suspend fun addList(name: String, sortOrder: Int): Long {
        require(name.isNotBlank()) { "List name is required" }
        return listDao.insert(ListEntity(name = name.trim(), sortOrder = sortOrder))
    }

    override suspend fun renameList(listId: Long, newName: String) {
        require(newName.isNotBlank()) { "List name is required" }
        listDao.rename(listId, newName.trim())
    }

    override suspend fun deleteList(listId: Long) = db.withTransaction {
        val sqlite = db.openHelper.writableDatabase
        val sharingExists = sqlite.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='shopping_shared_access'").use { it.moveToFirst() }
        if (sharingExists) {
            val admin = sqlite.query("""SELECT 1 FROM shopping_shared_access a JOIN shopping_sync_map m
                ON m.entity='list' AND m.remote_id=a.remote_id WHERE m.local_id=?""", arrayOf(listId)).use { it.moveToFirst() }
            check(!admin) { "Only the Owner can delete a shared list" }
        }
        itemDao.deleteByList(listId)
        listDao.deleteById(listId)
    }

    override fun observeItems(listId: Long, kind: Int): Flow<List<ItemEntity>> =
        itemDao.observeByList(listId, kind)

    override suspend fun getItems(listId: Long, kind: Int): List<ItemEntity> =
        itemDao.getByList(listId, kind)

    override suspend fun addItem(listId: Long, title: String, kind: Int): Long = db.withTransaction {
        requireNotNull(listDao.getById(listId)) { "List no longer exists" }
        require(title.isNotBlank()) { "Product name is required" }
        itemDao.insert(
            ItemEntity(listId = listId, title = title.trim(), kind = kind,
                category = "Other", qty = 1, checked = false, isActive = true)
        )
    }

    override suspend fun updateItem(item: ItemEntity) {
        require(item.qty >= 1 && item.title.isNotBlank())
        itemDao.update(item)
    }

    override suspend fun setQty(id: Long, qty: Int) {
        require(qty >= 1)
        itemDao.setQty(id, qty)
    }

    override suspend fun changeQty(id: Long, delta: Int) {
        require(delta == 1 || delta == -1)
        itemDao.changeQty(id, delta)
    }

    override suspend fun toggleChecked(id: Long, checked: Boolean) = itemDao.setChecked(id, checked)
    override suspend fun deleteItemSoft(id: Long) = itemDao.setActive(id, false)
}
