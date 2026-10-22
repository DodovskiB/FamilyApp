package repo

import android.net.Uri

import kotlinx.coroutines.flow.Flow
import com.example.familyapp.data.entity.ItemEntity
import com.example.familyapp.data.entity.ListEntity
import com.example.familyapp.data.entity.CatalogItemEntity

interface FamilyRepository {

    fun observeCatalogItems(): Flow<List<CatalogItemEntity>>

    suspend fun addCatalogItem(title: String, category: String): Long

    suspend fun updateCatalogItem(item: CatalogItemEntity)

    suspend fun deleteCatalogItem(id: Long)

    suspend fun attachCatalogPhoto(id: Long, uri: Uri)

    suspend fun removeCatalogPhoto(id: Long)

    suspend fun setItemLink(id: Long, url: String)
    suspend fun attachItemPhoto(id: Long, uri: Uri)
    suspend fun removeItemPhoto(id: Long)

    suspend fun setCatalogLink(id: Long, url: String)

    suspend fun addCatalogItemToList(listId: Long, catalogItemId: Long, purchasedQuantity: Int? = null): Long

    suspend fun clearPurchased(listId: Long)

    suspend fun createProductForList(listId: Long, title: String, category: String): Long

    suspend fun changeQty(id: Long, delta: Int)

    /* ---------- LISTS ---------- */

    fun observeLists(): Flow<List<ListEntity>>

    suspend fun getLists(): List<ListEntity>

    suspend fun addList(
        name: String,
        sortOrder: Int = 0
    ): Long

    suspend fun renameList(
        listId: Long,
        newName: String
    )

    suspend fun deleteList(
        listId: Long
    )

    /* ---------- ITEMS ---------- */

    fun observeItems(
        listId: Long,
        kind: Int
    ): Flow<List<ItemEntity>>

    suspend fun getItems(
        listId: Long,
        kind: Int
    ): List<ItemEntity>

    suspend fun addItem(
        listId: Long,
        title: String,
        kind: Int
    ): Long

    suspend fun updateItem(
        item: ItemEntity
    )

    suspend fun setQty(
        id: Long,
        qty: Int
    )

    suspend fun toggleChecked(
        id: Long,
        checked: Boolean
    )

    suspend fun deleteItemSoft(
        id: Long
    )
}

class PurchasedProductNeedsConfirmation(val productId: Long) : Exception("Product is already purchased")
