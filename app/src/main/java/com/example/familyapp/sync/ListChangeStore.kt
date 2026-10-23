package com.example.familyapp.sync

import androidx.room.withTransaction
import com.example.familyapp.data.db.AppDatabase

/** Local read watermarks belong to this account database and this phone only. */
class ListChangeStore(private val room: AppDatabase) {
    private suspend fun prepare() = room.withTransaction {
        room.openHelper.writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS shopping_list_seen (remote_id TEXT PRIMARY KEY NOT NULL, latest INTEGER NOT NULL, seen INTEGER NOT NULL)")
    }

    suspend fun update(revisions: Map<String, Long>, openRemoteId: String?) {
        prepare()
        room.withTransaction {
            val db = room.openHelper.writableDatabase
            val previous = db.query("SELECT remote_id FROM shopping_list_seen").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
            (previous - revisions.keys).forEach { db.execSQL("DELETE FROM shopping_list_seen WHERE remote_id=?", arrayOf(it)) }
            revisions.forEach { (id, revision) ->
                require(revision >= 0)
                db.execSQL("INSERT OR IGNORE INTO shopping_list_seen VALUES (?,0,0)", arrayOf(id))
                db.execSQL("UPDATE shopping_list_seen SET latest=max(latest,?) WHERE remote_id=?", arrayOf<Any>(revision,id))
                if (id == openRemoteId) db.execSQL("UPDATE shopping_list_seen SET seen=latest WHERE remote_id=?", arrayOf(id))
            }
        }
    }

    suspend fun markSeen(remoteId: String) {
        prepare()
        room.withTransaction { room.openHelper.writableDatabase.execSQL("UPDATE shopping_list_seen SET seen=latest WHERE remote_id=?", arrayOf(remoteId)) }
    }

    suspend fun unread(mappings: Map<Long, String>): Set<Long> {
        prepare()
        return room.withTransaction {
            val remote = room.openHelper.readableDatabase.query("SELECT remote_id FROM shopping_list_seen WHERE latest>seen").use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
            mappings.filterValues { it in remote }.keys
        }
    }
}
