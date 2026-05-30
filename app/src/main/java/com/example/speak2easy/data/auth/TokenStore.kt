package com.example.speak2easy.data.auth

import android.content.Context
import com.example.speak2easy.domain.model.User
import kotlinx.serialization.json.Json

/**
 * Stores the JWT and cached user in app-private SharedPreferences. Synchronous so the
 * OkHttp [com.example.speak2easy.data.remote.AuthInterceptor] can read the token without
 * blocking on coroutines.
 *
 * NOTE: plaintext app-private storage for the MVP — encrypt before release (hardening TODO).
 */
class TokenStore(context: Context, private val json: Json) {

    private val prefs = context.applicationContext
        .getSharedPreferences("sonari_auth", Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getCachedUser(): User? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return try {
            json.decodeFromString(User.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    fun saveSession(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, json.encodeToString(User.serializer(), user))
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    private companion object {
        const val KEY_TOKEN = "sonari_jwt_token"
        const val KEY_USER = "sonari_current_user"
    }
}
