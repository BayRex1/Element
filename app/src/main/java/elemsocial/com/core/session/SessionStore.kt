package elemsocial.com.core.session

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("element_session", Context.MODE_PRIVATE)

    fun getSessionKey(): String? {
        return prefs.getString(KEY_S_KEY, null)
    }

    fun saveSessionKey(value: String) {
        prefs.edit().putString(KEY_S_KEY, value).apply()
    }

    fun clearSessionKey() {
        prefs.edit().remove(KEY_S_KEY).apply()
    }

    private companion object {
        private const val KEY_S_KEY = "s_key"
    }
}
