package ru.mpavel.osmmap.core.net

import android.content.Context

/** Токен сессии API (живёт на сервере 12 ч). Пароль не сохраняется. */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    @Volatile
    var token: String? = prefs.getString(KEY_TOKEN, null)
        private set

    fun save(token: String) {
        this.token = token
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clear() {
        token = null
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
