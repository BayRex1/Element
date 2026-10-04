package elemsocial.com.core.settings

import android.content.Context
import elemsocial.com.core.model.TransparencyMode

enum class AppThemeMode(val value: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    Amoled("amoled");

    companion object {
        fun from(value: String?): AppThemeMode {
            return entries.firstOrNull { it.value == value } ?: System
        }
    }
}

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("element_settings", Context.MODE_PRIVATE)

    fun getThemeMode(): AppThemeMode {
        return AppThemeMode.from(prefs.getString(KEY_THEME_MODE, AppThemeMode.Dark.value))
    }

    fun saveThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.value).apply()
    }

    fun getDefaultFeed(): String {
        return prefs.getString(KEY_DEFAULT_FEED, "last") ?: "last"
    }

    fun saveDefaultFeed(value: String) {
        prefs.edit().putString(KEY_DEFAULT_FEED, value).apply()
    }

    fun getNotificationsToastEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_TOAST, true)
    }

    fun saveNotificationsToastEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_TOAST, value).apply()
    }

    fun getVideoPlayerVolume(): Float {
        return prefs.getFloat(KEY_VIDEO_PLAYER_VOLUME, 1f).coerceIn(0f, 1f)
    }

    fun saveVideoPlayerVolume(value: Float) {
        prefs.edit()
            .putFloat(KEY_VIDEO_PLAYER_VOLUME, value.coerceIn(0f, 1f))
            .apply()
    }

    fun getAutoVideoDownloadEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_VIDEO_DOWNLOAD, false)
    }

    fun saveAutoVideoDownloadEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_VIDEO_DOWNLOAD, value).apply()
    }

    fun getVideoAutoplayEnabled(): Boolean {
        return prefs.getBoolean(KEY_VIDEO_AUTOPLAY, false)
    }

    fun saveVideoAutoplayEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_AUTOPLAY, value).apply()
    }

    fun getTransparencyMode(): TransparencyMode {
        val rawValue = prefs.getString(KEY_TRANSPARENCY_MODE, TransparencyMode.ADAPTIVE.name)
        return runCatching { TransparencyMode.valueOf(rawValue.orEmpty()) }
            .getOrDefault(TransparencyMode.ADAPTIVE)
    }

    fun saveTransparencyMode(mode: TransparencyMode) {
        prefs.edit().putString(KEY_TRANSPARENCY_MODE, mode.name).apply()
    }

    private companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DEFAULT_FEED = "default_feed"
        private const val KEY_NOTIFICATIONS_TOAST = "notifications_toast"
        private const val KEY_VIDEO_PLAYER_VOLUME = "video_player_volume"
        private const val KEY_AUTO_VIDEO_DOWNLOAD = "auto_video_download"
        private const val KEY_VIDEO_AUTOPLAY = "video_autoplay"
        private const val KEY_TRANSPARENCY_MODE = "transparency_mode"
    }
}
