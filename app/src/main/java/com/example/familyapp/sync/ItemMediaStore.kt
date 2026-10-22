package com.example.familyapp.sync

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.familyapp.data.db.AppDatabase
import kotlinx.serialization.json.*
import java.util.UUID

/** Separate durable, field-level attachment queue; existing quantity/check operations stay unchanged. */
class ItemMediaStore(private val room: AppDatabase) {
    private suspend fun <T> tx(block: (SupportSQLiteDatabase) -> T): T = room.withTransaction { block(room.openHelper.writableDatabase) }

    suspend fun prepare() = tx { db ->
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_media_seen (item_id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS shopping_media_outbox (
            seq INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, operation_id TEXT NOT NULL UNIQUE,
            item_id INTEGER NOT NULL, mask INTEGER NOT NULL, link_url TEXT, photo_file TEXT, photo_path TEXT)""")
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_revoked_media AS SELECT * FROM shopping_media_outbox WHERE 0")
        val enabled = "(SELECT value FROM shopping_sync_meta WHERE name='enabled')='1' AND (SELECT value FROM shopping_sync_meta WHERE name='applying')='0'"
        db.execSQL("""CREATE TRIGGER IF NOT EXISTS shopping_media_insert AFTER INSERT ON items
            WHEN $enabled AND (NEW.linkUrl IS NOT NULL OR NEW.photoFileName IS NOT NULL OR NEW.photoRemotePath IS NOT NULL)
            BEGIN ${enqueue("3")}; END""")
        val mask = "(OLD.linkUrl IS NOT NEW.linkUrl)*2 + ((OLD.photoFileName IS NOT NEW.photoFileName) OR (OLD.photoRemotePath IS NOT NEW.photoRemotePath))*1"
        db.execSQL("""CREATE TRIGGER IF NOT EXISTS shopping_media_update AFTER UPDATE ON items
            WHEN $enabled AND ($mask) != 0 BEGIN ${enqueue(mask)}; END""")
    }

    suspend fun seed(snapshot: JsonArray) = tx { db ->
        val cloud = snapshot.associate { it.jsonObject.getValue("id").jsonPrimitive.content to it.jsonObject }
        db.query("""SELECT i.*,m.remote_id FROM items i JOIN shopping_sync_map m ON m.entity='item' AND m.local_id=i.id
            WHERE NOT EXISTS(SELECT 1 FROM shopping_media_seen s WHERE s.item_id=i.id)""").use { c ->
            while (c.moveToNext()) {
                fun text(name: String): String? = c.getColumnIndexOrThrow(name).let { if(c.isNull(it)) null else c.getString(it) }
                val remote = cloud[text("remote_id")] ?: continue
                val id = c.getLong(c.getColumnIndexOrThrow("id"))
                val pendingMask = db.query("SELECT mask FROM shopping_media_outbox WHERE item_id=?", arrayOf<Any?>(id)).use { cursor ->
                    var mask = 0; while (cursor.moveToNext()) mask = mask or cursor.getInt(0); mask
                }
                if (!remote.getValue("has_media").jsonPrimitive.boolean) {
                    val photoMask = if ((text("photoFileName") != null || text("photoRemotePath") != null) && pendingMask and 1 == 0) 1 else 0
                    val linkMask = if (text("linkUrl") != null && pendingMask and 2 == 0) 2 else 0
                    if (photoMask or linkMask != 0) db.execSQL("INSERT INTO shopping_media_outbox(operation_id,item_id,mask,link_url,photo_file,photo_path) VALUES(?,?,?,?,?,?)",
                        arrayOf<Any?>(UUID.randomUUID().toString(), id, photoMask or linkMask, text("linkUrl"), text("photoFileName"), text("photoRemotePath")))
                }
                db.execSQL("INSERT OR IGNORE INTO shopping_media_seen VALUES(?)", arrayOf<Any?>(id))
            }
        }
    }

    suspend fun pending(): List<MediaChange> = tx { db ->
        // Remote removal has already been reconciled by normal sync. Keep rejected work private.
        db.execSQL("INSERT INTO shopping_revoked_media SELECT * FROM shopping_media_outbox WHERE item_id NOT IN (SELECT id FROM items)")
        db.execSQL("DELETE FROM shopping_media_outbox WHERE item_id NOT IN (SELECT id FROM items)")
        db.query("SELECT o.*,m.remote_id FROM shopping_media_outbox o JOIN shopping_sync_map m ON m.entity='item' AND m.local_id=o.item_id ORDER BY o.seq LIMIT 100").use { c ->
            buildList { while (c.moveToNext()) {
                fun text(name: String): String? = c.getColumnIndexOrThrow(name).let { if(c.isNull(it)) null else c.getString(it) }
                add(MediaChange(text("operation_id")!!, c.getLong(c.getColumnIndexOrThrow("item_id")), text("remote_id")!!,
                    text("mask")!!.toInt(), text("link_url"), text("photo_file"), text("photo_path")))
            } }
        }
    }

    suspend fun acknowledge(change: MediaChange, path: String?) = tx { db ->
        db.execSQL("DELETE FROM shopping_media_outbox WHERE operation_id=?", arrayOf<Any?>(change.operationId))
        // Do not attach a stale upload to an image edited/replaced while that request was running.
        if (change.mask and 1 != 0) applying(db) {
            db.execSQL("UPDATE items SET photoRemotePath=? WHERE id=? AND photoFileName IS ? AND photoRemotePath IS ?",
                arrayOf<Any?>(path, change.localId, change.photoFile, change.photoPath))
        }
    }

    suspend fun apply(snapshot: JsonArray): List<String> = tx { db ->
        val released = mutableListOf<String>()
        applying(db) {
        snapshot.forEach { value ->
            val row = value.jsonObject
            val remote = row.getValue("id").jsonPrimitive.content
            fun text(key: String) = row[key]?.jsonPrimitive?.contentOrNull
            val id = db.query("SELECT local_id FROM shopping_sync_map WHERE entity='item' AND remote_id=?", arrayOf<Any?>(remote)).use { if(it.moveToFirst()) it.getLong(0) else null } ?: return@forEach
            // Pending fields win locally until their own server operation has been acknowledged.
            val mask = db.query("SELECT mask FROM shopping_media_outbox WHERE item_id=?", arrayOf<Any?>(id)).use { c -> var m=0; while(c.moveToNext()) m=m or c.getInt(0); m }
            if (mask and 2 == 0) db.execSQL("UPDATE items SET linkUrl=? WHERE id=?", arrayOf<Any?>(text("link_url"),id))
            if (mask and 1 == 0) {
                db.query("SELECT photoFileName,photoRemotePath FROM items WHERE id=?", arrayOf<Any?>(id)).use {
                    if (it.moveToFirst() && !it.isNull(0) && (text("photo_path") == null || it.getString(1) != text("photo_path"))) released.add(it.getString(0))
                }
                db.execSQL("""UPDATE items SET photoFileName=CASE WHEN ? IS NOT NULL AND photoRemotePath IS ? THEN photoFileName ELSE NULL END,
                    photoRemotePath=? WHERE id=?""", arrayOf<Any?>(text("photo_path"), text("photo_path"), text("photo_path"),id))
            }
        }
        }
        released
    }

    suspend fun invalidateMissingFile(id: Long, path: String, name: String) = tx { db -> applying(db) {
        db.execSQL("UPDATE items SET photoFileName=NULL WHERE id=? AND photoRemotePath=? AND photoFileName=?", arrayOf<Any?>(id,path,name))
    } }

    suspend fun localPhotos(): List<Triple<Long,String,String>> = tx { db ->
        db.query("SELECT id,photoRemotePath,photoFileName FROM items WHERE photoRemotePath IS NOT NULL AND photoFileName IS NOT NULL AND isActive=1").use { c ->
            buildList { while(c.moveToNext()) add(Triple(c.getLong(0),c.getString(1),c.getString(2))) }
        }
    }

    suspend fun missingPhotos(): List<Pair<Long,String>> = tx { db ->
        db.query("SELECT id,photoRemotePath FROM items WHERE photoRemotePath IS NOT NULL AND photoFileName IS NULL AND isActive=1").use { c ->
            buildList { while(c.moveToNext()) add(c.getLong(0) to c.getString(1)) }
        }
    }

    suspend fun cachePhoto(id: Long, path: String, name: String) = tx { db -> applying(db) {
        db.execSQL("UPDATE items SET photoFileName=? WHERE id=? AND photoRemotePath=? AND photoFileName IS NULL", arrayOf<Any?>(name,id,path))
    } }

    suspend fun referencedPhoto(name: String): Boolean = tx { db ->
        db.query("""SELECT 1 FROM items WHERE photoFileName=? UNION ALL SELECT 1 FROM catalog_items WHERE photoFileName=?
            UNION ALL SELECT 1 FROM shopping_media_outbox WHERE photo_file=? UNION ALL SELECT 1 FROM shopping_revoked_media WHERE photo_file=? LIMIT 1""",
            arrayOf<Any?>(name,name,name,name)).use { it.moveToFirst() }
    }

    suspend fun count(): Int = tx { db -> db.query("SELECT count(*) FROM shopping_media_outbox WHERE item_id IN (SELECT id FROM items)").use { it.moveToFirst();it.getInt(0) } }

    private fun applying(db: SupportSQLiteDatabase, block: () -> Unit) {
        db.execSQL("UPDATE shopping_sync_meta SET value='1' WHERE name='applying'")
        try { block() } finally { db.execSQL("UPDATE shopping_sync_meta SET value='0' WHERE name='applying'") }
    }
    private fun enqueue(mask: String) = """INSERT INTO shopping_media_outbox(operation_id,item_id,mask,link_url,photo_file,photo_path)
        VALUES(lower(hex(randomblob(16))),NEW.id,$mask,NEW.linkUrl,NEW.photoFileName,NEW.photoRemotePath)"""
}

data class MediaChange(val operationId: String, val localId: Long, val remoteId: String, val mask: Int,
                       val linkUrl: String?, val photoFile: String?, val photoPath: String?)
