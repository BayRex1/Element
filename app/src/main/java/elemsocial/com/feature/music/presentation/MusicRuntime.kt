package elemsocial.com.feature.music.presentation

import android.content.Context
import elemsocial.com.config.AppConfig
import elemsocial.com.core.cache.ImageDiskCache
import elemsocial.com.core.cache.MusicCacheIndexStore
import elemsocial.com.core.session.SessionStore
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.repository.MusicRepositoryImpl
import elemsocial.com.data.repository.MusicStorageRepositoryImpl

class MusicRuntime private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val socketClient = ElementSocketClient()
    private val imageDiskCache = ImageDiskCache(appContext)
    private val musicCacheIndexStore = MusicCacheIndexStore(appContext)
    private val musicRepository = MusicRepositoryImpl(socketClient)
    private val musicStorageRepository = MusicStorageRepositoryImpl(socketClient)

    val gateway = MusicGateway(
        musicRepository = musicRepository,
        musicStorageRepository = musicStorageRepository
    )

    val controller = MusicController(
        context = appContext,
        gateway = gateway,
        diskCache = imageDiskCache,
        cacheIndexStore = musicCacheIndexStore
    )

    init {
        updateSessionKey(SessionStore(appContext).getSessionKey())
    }

    fun updateSessionKey(rawSessionKey: String?) {
        val normalized = rawSessionKey
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        socketClient.setAuthorizationSessionKey(normalized)
        if (normalized != null) {
            socketClient.connect(AppConfig.Domains.DEFAULT_USER_WS_URLS)
        }
    }

    fun clearSession() {
        socketClient.setAuthorizationSessionKey(null)
        socketClient.disconnect(clearQueue = true, disableReconnect = true)
        controller.stopAndReset()
    }

    companion object {
        @Volatile
        private var instance: MusicRuntime? = null

        fun get(context: Context): MusicRuntime {
            return instance ?: synchronized(this) {
                instance ?: MusicRuntime(context).also { instance = it }
            }
        }

        fun peek(): MusicRuntime? = instance
    }
}
