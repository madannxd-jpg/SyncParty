package com.syncparty.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class UserSessionManager(private val context: Context) {

    private val dbHelper = SyncPartyDbHelper.getInstance(context)

    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
        private val KEY_UPDATE_URL = stringPreferencesKey("update_url")

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val displayNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_DISPLAY_NAME] ?: "Guest User" }
    val emailFlow: Flow<String> = context.dataStore.data.map { it[KEY_EMAIL] ?: "" }
    val avatarUrlFlow: Flow<String> = context.dataStore.data.map { it[KEY_AVATAR_URL] ?: "" }
    val updateUrlFlow: Flow<String> = context.dataStore.data.map {
        it[KEY_UPDATE_URL] ?: "https://raw.githubusercontent.com/madannxd-jpg/SyncParty/main/version.json"
    }

    suspend fun saveSession(user: UserProfile) {
        dbHelper.saveUser(user)
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_ID] = user.userId
            prefs[KEY_DISPLAY_NAME] = user.displayName
            prefs[KEY_EMAIL] = user.email
            prefs[KEY_AVATAR_URL] = user.avatarUrl
        }
    }

    suspend fun clearSession() {
        val currentUserId = context.dataStore.data.map { it[KEY_USER_ID] }.firstOrNull()
        currentUserId?.let { dbHelper.deleteUser(it) }
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_DISPLAY_NAME)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_AVATAR_URL)
        }
    }

    suspend fun setUpdateUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UPDATE_URL] = url
        }
    }

    suspend fun getUserProfile(): UserProfile? {
        val uid = context.dataStore.data.map { it[KEY_USER_ID] }.firstOrNull() ?: return null
        return dbHelper.getUser(uid)
    }
}
