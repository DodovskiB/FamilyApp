package com.example.familyapp.sync

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.familyapp.data.db.AppDatabase
import kotlinx.serialization.json.*

/** Auxiliary tables leave the existing Room entities and their local IDs unchanged. */
class SyncStore(private val room: AppDatabase) {
    private suspend fun <T> transaction(block: (SupportSQLiteDatabase) -> T): T =
        room.withTransaction { block(room.openHelper.writableDatabase) }

    suspend fun prepare() = transaction { db ->
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_sync_meta (name TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        listOf("applying" to "0", "enabled" to "0", "seeded" to "0", "downloaded" to "0", "cursor" to "0").forEach {
            db.execSQL("INSERT OR IGNORE INTO shopping_sync_meta VALUES (?, ?)", arrayOf(it.first, it.second))
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_sync_map (entity TEXT NOT NULL, local_id INTEGER NOT NULL, remote_id TEXT NOT NULL UNIQUE, initialized INTEGER NOT NULL, PRIMARY KEY(entity,local_id))")
        db.execSQL("""CREATE TABLE IF NOT EXISTS shopping_sync_outbox (
            seq INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, operation_id TEXT NOT NULL UNIQUE,
            entity TEXT NOT NULL, remote_id TEXT NOT NULL, action TEXT NOT NULL, mask INTEGER NOT NULL,
            title TEXT, category TEXT, qty INTEGER, checked INTEGER, active INTEGER,
            sort_order INTEGER, parent_id TEXT, catalog_id TEXT, link_url TEXT, delta INTEGER)""")
        // Triggers capture mutations in the same transaction as the existing DAO writes.
        definitions.forEach { definition ->
            val (table, entity) = definition
            listOf("INSERT", "UPDATE", "DELETE").forEach { action ->
                val row = if (action == "DELETE") "OLD" else "NEW"
                val mask = if (action != "UPDATE") "255" else mask(entity)
                val condition = "(SELECT value FROM shopping_sync_meta WHERE name='enabled')='1' AND " +
                    "(SELECT value FROM shopping_sync_meta WHERE name='applying')='0'" +
                    if (action == "UPDATE") " AND ($mask) != 0" else ""
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS shopping_sync_${entity}_${action.lowercase()}
                    AFTER $action ON $table WHEN $condition BEGIN
                    INSERT OR IGNORE INTO shopping_sync_map VALUES ('$entity', $row.id, $uuidSql, 1);
                    ${enqueue(entity, row, action.lowercase(), mask)};
                    END""")
            }
        }
    }

    suspend fun isSeeded(): Boolean = transaction { value(it, "seeded") == "1" }
    suspend fun hasInitialDownload(): Boolean = transaction { value(it, "downloaded") == "1" }
    suspend fun finishInitialDownload() = transaction { setValue(it, "downloaded", "1") }
    suspend fun cursor(): Long = transaction { value(it, "cursor").toLong() }
    suspend fun pendingCount(): Int = transaction { db ->
        db.query("SELECT count(*) FROM shopping_sync_outbox").use { it.moveToFirst(); it.getInt(0) }
    }

    suspend fun seed() = transaction { db ->
        if (value(db, "seeded") == "1") return@transaction
        definitions.forEach { (table, entity) ->
            db.execSQL("INSERT OR IGNORE INTO shopping_sync_map SELECT '$entity', id, $uuidSql, 0 FROM $table")
        }
        definitions.forEach { (table, entity) ->
            db.execSQL(enqueue(entity, "r", "insert", "255", " FROM $table r JOIN shopping_sync_map m ON m.entity='$entity' AND m.local_id=r.id WHERE m.initialized=0"))
        }
        db.execSQL("UPDATE shopping_sync_map SET initialized=1")
        setValue(db, "seeded", "1")
        setValue(db, "enabled", "1")
    }

    /** Only discard untouched, empty installation defaults when an account already has cloud data. */
    suspend fun discardPristineDefaults() = transaction { db ->
        val counts = listOf("items", "catalog_items").map { table ->
            db.query("SELECT count(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        }
        val lists = db.query("SELECT name, sortOrder FROM lists ORDER BY sortOrder,id").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getInt(1)) }
        }
        if (counts.all { it == 0 } && lists == listOf("Home" to 0, "Weekend House" to 1, "Vacation" to 2)) {
            val edited = db.query("SELECT 1 FROM shopping_sync_outbox WHERE entity!='list' OR action!='insert' LIMIT 1").use { it.moveToFirst() }
            if (edited) return@transaction
            setValue(db, "applying", "1")
            try {
                db.execSQL("DELETE FROM shopping_sync_outbox WHERE entity='list'")
                db.execSQL("DELETE FROM shopping_sync_map WHERE entity='list'")
                db.execSQL("DELETE FROM lists")
            } finally { setValue(db, "applying", "0") }
        }
    }

    suspend fun pending(limit: Int = 100): List<QueuedChange> = transaction { db ->
        db.query("SELECT * FROM shopping_sync_outbox ORDER BY seq LIMIT $limit").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    fun text(name: String): String? = cursor.getColumnIndexOrThrow(name).let {
                        if (cursor.isNull(it)) null else cursor.getString(it)
                    }
                    val entity = text("entity")!!
                    val action = text("action")!!
                    val mask = text("mask")!!.toInt()
                    val body = buildJsonObject {
                        fun stringField(key: String, column: String = key) { put(key, text(column)?.let(::JsonPrimitive) ?: JsonNull) }
                        if (action != "delete") {
                            if (mask and 1 != 0) stringField(if (entity == "list") "name" else "title", "title")
                            if (entity == "list") {
                                if (mask and 2 != 0) put("sort_order", text("sort_order")!!.toInt())
                            } else {
                                if (mask and 2 != 0) stringField("category")
                                if (entity == "catalog" && mask and 32 != 0) stringField("link_url")
                                if (entity == "item") {
                                    if (mask and 4 != 0) {
                                        if (action == "insert") put("qty", text("qty")!!.toInt())
                                        else put("qty_delta", text("delta")!!.toLong())
                                    }
                                    if (mask and 8 != 0) put("checked", text("checked") == "1")
                                    if (mask and 16 != 0) put("active", text("active") == "1")
                                    if (mask and 64 != 0) { stringField("list_id", "parent_id"); stringField("catalog_id") }
                                }
                            }
                        }
                    }
                    add(QueuedChange(text("operation_id")!!, buildJsonObject {
                        put("operation_id", text("operation_id")); put("entity", entity)
                        put("id", text("remote_id")); put("action", action); put("body", body)
                    }))
                }
            }
        }
    }

    suspend fun acknowledge(ids: List<String>) = transaction { db ->
        ids.forEach { db.execSQL("DELETE FROM shopping_sync_outbox WHERE operation_id=?", arrayOf(it)) }
    }

    suspend fun apply(records: JsonArray) = transaction { db ->
        setValue(db, "applying", "1")
        var blocked = false
        var revision = value(db, "cursor").toLong()
        try {
            records.sortedBy { listOf("list", "catalog", "item").indexOf(it.jsonObject.getValue("entity").jsonPrimitive.content) }.forEach { element ->
                val record = element.jsonObject
                val id = record.getValue("id").jsonPrimitive.content
                val entity = record.getValue("entity").jsonPrimitive.content
                val pending = db.query("SELECT 1 FROM shopping_sync_outbox WHERE remote_id=? LIMIT 1", arrayOf(id)).use { it.moveToFirst() }
                if (pending) blocked = true else applyRecord(db, entity, id, record)
                revision = maxOf(revision, record.getValue("revision").jsonPrimitive.long)
            }
            // Re-read skipped rows after queued local operations have been acknowledged.
            if (!blocked) setValue(db, "cursor", revision.toString())
        } finally { setValue(db, "applying", "0") }
    }

    private fun applyRecord(db: SupportSQLiteDatabase, entity: String, id: String, record: JsonObject) {
        require(entity in listOf("list", "catalog", "item"))
        var local = localId(db, entity, id)
        val table = definitions.first { it.second == entity }.first
        if (record.getValue("deleted").jsonPrimitive.boolean) {
            if (local != null) {
                if (entity == "list") db.execSQL("DELETE FROM items WHERE listId=?", arrayOf(local))
                if (entity == "catalog") db.execSQL("UPDATE items SET catalogItemId=NULL WHERE catalogItemId=?", arrayOf(local))
                db.execSQL("DELETE FROM $table WHERE id=?", arrayOf(local))
            }
            return
        }
        val data = record.getValue("body").jsonObject
        fun text(key: String) = data[key]?.jsonPrimitive?.contentOrNull
        val values = android.content.ContentValues()
        when (entity) {
            "list" -> {
                values.put("name", text("name")!!)
                values.put("sortOrder", data.getValue("sort_order").jsonPrimitive.int)
            }
            "catalog" -> {
                values.put("title", text("title")!!); values.put("category", text("category")!!)
                values.put("linkUrl", text("link_url"))
            }
            "item" -> {
                val parent = localId(db, "list", text("list_id")!!) ?: error("Missing parent list")
                values.put("listId", parent); values.put("title", text("title")!!)
                values.put("category", text("category")!!); values.put("kind", 0)
                values.put("qty", data.getValue("qty").jsonPrimitive.int)
                values.put("checked", data.getValue("checked").jsonPrimitive.boolean)
                values.put("isActive", data.getValue("active").jsonPrimitive.boolean)
                values.put("catalogItemId", text("catalog_id")?.let { localId(db, "catalog", it) })
            }
        }
        val exists = local != null && db.query("SELECT 1 FROM $table WHERE id=?", arrayOf(local)).use { it.moveToFirst() }
        if (exists) db.update(table, android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT, values, "id=?", arrayOf(local))
        else {
            local?.let { values.put("id", it) }
            local = db.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT, values)
            db.execSQL("INSERT OR REPLACE INTO shopping_sync_map VALUES (?, ?, ?, 1)", arrayOf<Any?>(entity, local, id))
        }
    }

    private fun localId(db: SupportSQLiteDatabase, entity: String, id: String): Long? =
        db.query("SELECT local_id FROM shopping_sync_map WHERE entity=? AND remote_id=?", arrayOf(entity, id)).use {
            if (it.moveToFirst()) it.getLong(0) else null
        }

    private fun value(db: SupportSQLiteDatabase, name: String): String =
        db.query("SELECT value FROM shopping_sync_meta WHERE name=?", arrayOf(name)).use { it.moveToFirst(); it.getString(0) }

    private fun setValue(db: SupportSQLiteDatabase, name: String, value: String) =
        db.execSQL("UPDATE shopping_sync_meta SET value=? WHERE name=?", arrayOf(value, name))

    companion object {
        private val definitions = listOf("lists" to "list", "catalog_items" to "catalog", "items" to "item")
        private const val uuidSql = "lower(hex(randomblob(4)))||'-'||lower(hex(randomblob(2)))||'-'||lower(hex(randomblob(2)))||'-'||lower(hex(randomblob(2)))||'-'||lower(hex(randomblob(6)))"
        private fun reference(entity: String, id: String) = "(SELECT remote_id FROM shopping_sync_map WHERE entity='$entity' AND local_id=$id)"
        private fun mask(entity: String): String = when (entity) {
            "list" -> "(OLD.name IS NOT NEW.name)*1 + (OLD.sortOrder IS NOT NEW.sortOrder)*2"
            "catalog" -> "(OLD.title IS NOT NEW.title)*1 + (OLD.category IS NOT NEW.category)*2 + (OLD.linkUrl IS NOT NEW.linkUrl)*32"
            else -> "(OLD.title IS NOT NEW.title)*1 + (OLD.category IS NOT NEW.category)*2 + (OLD.qty IS NOT NEW.qty)*4 + (OLD.checked IS NOT NEW.checked)*8 + (OLD.isActive IS NOT NEW.isActive)*16 + ((OLD.listId IS NOT NEW.listId) OR (OLD.catalogItemId IS NOT NEW.catalogItemId))*64"
        }

        private fun enqueue(entity: String, row: String, action: String, mask: String, from: String = ""): String {
            val title = "$row.${if (entity == "list") "name" else "title"}"
            val category = if (entity == "list") "NULL" else "$row.category"
            val qty = if (entity == "item") "$row.qty" else "NULL"
            val checked = if (entity == "item") "$row.checked" else "NULL"
            val active = if (entity == "item") "$row.isActive" else "NULL"
            val sort = if (entity == "list") "$row.sortOrder" else "NULL"
            val parent = if (entity == "item") reference("list", "$row.listId") else "NULL"
            val catalog = if (entity == "item") reference("catalog", "$row.catalogItemId") else "NULL"
            val link = if (entity == "catalog") "$row.linkUrl" else "NULL"
            val delta = if (entity == "item" && action == "update") "NEW.qty - OLD.qty" else "NULL"
            return "INSERT INTO shopping_sync_outbox (operation_id,entity,remote_id,action,mask,title,category,qty,checked,active,sort_order,parent_id,catalog_id,link_url,delta) SELECT " +
                "$uuidSql, '$entity', ${reference(entity, "$row.id")}, '$action', $mask, $title, $category, $qty, $checked, $active, $sort, $parent, $catalog, $link, $delta$from"
        }
    }
}

data class QueuedChange(val operationId: String, val json: JsonObject)
