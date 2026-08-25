package com.syncparty.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.syncparty.app.model.ChatMessage
import com.syncparty.app.model.StreamMode

data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val idToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class RoomHistoryItem(
    val roomId: String,
    val roomName: String,
    val hostName: String,
    val lastJoined: Long,
    val activeMode: StreamMode,
    val isFavorite: Boolean = false
)

class SyncPartyDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Users Table
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                user_id TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                email TEXT NOT NULL,
                avatar_url TEXT,
                id_token TEXT,
                created_at INTEGER
            )
        """.trimIndent())

        // 2. Room History Table
        db.execSQL("""
            CREATE TABLE $TABLE_ROOM_HISTORY (
                room_id TEXT PRIMARY KEY,
                room_name TEXT NOT NULL,
                host_name TEXT NOT NULL,
                last_joined INTEGER NOT NULL,
                active_mode TEXT NOT NULL,
                is_favorite INTEGER DEFAULT 0
            )
        """.trimIndent())

        // 3. Chat Cache Table
        db.execSQL("""
            CREATE TABLE $TABLE_CHAT_CACHE (
                message_id TEXT PRIMARY KEY,
                room_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                text TEXT,
                attachment_type TEXT,
                attachment_path TEXT,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROOM_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_CACHE")
        onCreate(db)
    }

    // --- User Profile Queries ---
    fun saveUser(user: UserProfile) {
        val values = ContentValues().apply {
            put("user_id", user.userId)
            put("display_name", user.displayName)
            put("email", user.email)
            put("avatar_url", user.avatarUrl)
            put("id_token", user.idToken)
            put("created_at", user.createdAt)
        }
        writableDatabase.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUser(userId: String): UserProfile? {
        val cursor = readableDatabase.query(
            TABLE_USERS, null, "user_id = ?", arrayOf(userId), null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                UserProfile(
                    userId = it.getString(it.getColumnIndexOrThrow("user_id")),
                    displayName = it.getString(it.getColumnIndexOrThrow("display_name")),
                    email = it.getString(it.getColumnIndexOrThrow("email")),
                    avatarUrl = it.getString(it.getColumnIndexOrThrow("avatar_url")) ?: "",
                    idToken = it.getString(it.getColumnIndexOrThrow("id_token")) ?: "",
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                )
            } else null
        }
    }

    fun deleteUser(userId: String) {
        writableDatabase.delete(TABLE_USERS, "user_id = ?", arrayOf(userId))
    }

    // --- Room History Queries ---
    fun saveRoomHistory(item: RoomHistoryItem) {
        val values = ContentValues().apply {
            put("room_id", item.roomId)
            put("room_name", item.roomName)
            put("host_name", item.hostName)
            put("last_joined", item.lastJoined)
            put("active_mode", item.activeMode.name)
            put("is_favorite", if (item.isFavorite) 1 else 0)
        }
        writableDatabase.insertWithOnConflict(TABLE_ROOM_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getRecentRooms(limit: Int = 10): List<RoomHistoryItem> {
        val list = mutableListOf<RoomHistoryItem>()
        val cursor = readableDatabase.query(
            TABLE_ROOM_HISTORY, null, null, null, null, null, "last_joined DESC", limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                val modeStr = it.getString(it.getColumnIndexOrThrow("active_mode"))
                val mode = try { StreamMode.valueOf(modeStr) } catch (e: Exception) { StreamMode.YOUTUBE }
                list.add(
                    RoomHistoryItem(
                        roomId = it.getString(it.getColumnIndexOrThrow("room_id")),
                        roomName = it.getString(it.getColumnIndexOrThrow("room_name")),
                        hostName = it.getString(it.getColumnIndexOrThrow("host_name")),
                        lastJoined = it.getLong(it.getColumnIndexOrThrow("last_joined")),
                        activeMode = mode,
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1
                    )
                )
            }
        }
        return list
    }

    companion object {
        const val DATABASE_NAME = "syncparty.db"
        const val DATABASE_VERSION = 1
        const val TABLE_USERS = "users"
        const val TABLE_ROOM_HISTORY = "room_history"
        const val TABLE_CHAT_CACHE = "chat_cache"

        @Volatile
        private var INSTANCE: SyncPartyDbHelper? = null

        fun getInstance(context: Context): SyncPartyDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncPartyDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
