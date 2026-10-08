package elemsocial.com.core.plugins

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import dalvik.system.DexClassLoader
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.ui.pack.theme.ElementUiPalette
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

const val ELEMENT_PLUGIN_API_VERSION = 2

data class ElementPlugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val icon: String,
    val iconBase64: String? = null,
    val iconMimeType: String? = null,
    val fileName: String,
    val settings: Map<String, String> = emptyMap(),
    val visualBalance: Double? = null,
    val visualHallRank: Int? = null,
    val visualDisplayName: String? = null,
    val visualUsername: String? = null,
    val useCurrentUser: Boolean = true,
    val entryClass: String? = null,
    val codeDexBase64: String? = null
)

interface ElementPluginEntry {
    fun onLoad(host: ElementPluginHost)
    fun onUnload() {}
}

interface ElementPluginEntryV2 : ElementPluginEntry {
    override fun onLoad(host: ElementPluginHost) { onLoadV2(host as ElementPluginHostV2) }
    fun onLoadV2(host: ElementPluginHostV2)
}

interface ElementPluginHost {
    fun registerBottomNavigation(item: ElementPluginBottomNavItem): ElementPluginRegistration
    fun openPluginManager()
}

interface ElementPluginHostV2 : ElementPluginHost {
    val apiVersion: Int get() = ELEMENT_PLUGIN_API_VERSION
    fun unregisterBottomNavigation(itemId: String)
    fun openPost(postId: Int): Boolean
    fun openProfile(username: String): Boolean
    fun showToast(message: String, long: Boolean = false)
    fun openUrl(url: String): Boolean
    fun currentUser(): ElementPluginUser
    fun appInfo(): ElementPluginAppInfo
    fun connectionState(): String
    fun setTheme(overrides: ElementPluginThemeOverrides)
    fun resetTheme()
    fun invalidateUi(reason: String = "plugin")
    fun storage(): ElementPluginStorage
    val serverEvents: SharedFlow<Map<String, Any?>>
    suspend fun serverRequest(
        type: String,
        action: String,
        payload: Map<String, Any?> = emptyMap(),
        timeoutMs: Long = 60_000
    ): ElementPluginServerResponse
}

fun interface ElementPluginAction { fun invoke() }

data class ElementPluginBottomNavItem(
    val id: String,
    val title: String,
    val icon: String = "🧩",
    val iconBase64: String? = null,
    val action: ElementPluginAction
) {
    var badgeCount: Int? = null

    constructor(
        id: String,
        title: String,
        icon: String = "🧩",
        iconBase64: String? = null,
        badgeCount: Int? = null,
        action: ElementPluginAction
    ) : this(id, title, icon, iconBase64, action) {
        this.badgeCount = badgeCount
    }
}

data class ElementPluginRegistration(val pluginId: String, val itemId: String)

data class ElementPluginNavItem(
    val pluginId: String,
    val itemId: String,
    val title: String,
    val icon: String,
    val iconBase64: String? = null,
    val badgeCount: Int? = null,
    val action: () -> Unit
)

data class ElementPluginUser(val id: Int?, val name: String?, val username: String?, val email: String?)
data class ElementPluginAppInfo(val apiVersion: Int, val applicationId: String, val versionName: String, val versionCode: Long)

data class ElementPluginThemeOverrides(
    val accentArgb: Int? = null,
    val bodyArgb: Int? = null,
    val blockArgb: Int? = null,
    val blockSoftArgb: Int? = null,
    val textPrimaryArgb: Int? = null,
    val textSecondaryArgb: Int? = null,
    val errorArgb: Int? = null,
    val successArgb: Int? = null,
    val infoArgb: Int? = null
)

data class ElementPluginServerResponse(
    val ok: Boolean,
    val status: String?,
    val message: String?,
    val data: Map<String, Any?>,
    val raw: Map<String, Any?>
)

class ElementPluginStorage internal constructor(private val preferences: android.content.SharedPreferences) {
    fun get(key: String, default: String? = null): String? = preferences.getString(key, default)
    fun put(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    fun remove(key: String) { preferences.edit().remove(key).apply() }
    fun clear() { preferences.edit().clear().apply() }
}

object ElementPluginRuntime {
    private data class Loaded(val plugin: ElementPlugin, val entry: ElementPluginEntry)
    private val loaded = CopyOnWriteArrayList<Loaded>()
    private val registrations = CopyOnWriteArrayList<ElementPluginNavItem>()
    private val _bottomNavigation = MutableStateFlow<List<ElementPluginNavItem>>(emptyList())
    val bottomNavigation: StateFlow<List<ElementPluginNavItem>> = _bottomNavigation.asStateFlow()
    private val _serverEvents = MutableSharedFlow<Map<String, Any?>>(extraBufferCapacity = 256)
    val serverEvents: SharedFlow<Map<String, Any?>> = _serverEvents.asSharedFlow()

    @Volatile private var manager: (() -> Unit)? = null
    @Volatile private var postOpener: ((Int) -> Boolean)? = null
    @Volatile private var profileOpener: ((String) -> Boolean)? = null
    @Volatile private var socket: ElementSocketClient? = null
    @Volatile private var currentUserValue = ElementPluginUser(null, null, null, null)
    @Volatile private var appInfoValue = ElementPluginAppInfo(2, "", "", 0)

    @Synchronized
    fun attach(
        context: Context,
        onOpenPluginManager: () -> Unit,
        socketClient: ElementSocketClient? = null,
        user: ElementPluginUser = ElementPluginUser(null, null, null, null),
        onOpenPost: (Int) -> Boolean = { false },
        onOpenProfile: (String) -> Boolean = { false }
    ) {
        unloadAll()
        val app = context.applicationContext
        manager = onOpenPluginManager
        socket = socketClient
        currentUserValue = user
        postOpener = onOpenPost
        profileOpener = onOpenProfile
        val pi = app.packageManager.getPackageInfo(app.packageName, 0)
        appInfoValue = ElementPluginAppInfo(
            ELEMENT_PLUGIN_API_VERSION,
            app.packageName,
            pi.versionName.orEmpty(),
            if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
        )
        ElementPluginStore(app).loadAll().forEach { loadPlugin(app, it) }
        publish()
    }

    @Synchronized fun reload(context: Context) {
        val callback = manager ?: return
        attach(context, callback, socket, currentUserValue, postOpener ?: { false }, profileOpener ?: { false })
    }

    @Synchronized fun detach() {
        unloadAll()
        manager = null
        postOpener = null
        profileOpener = null
        socket = null
    }

    fun setUser(user: ElementPluginUser) { currentUserValue = user }
    fun emitServerEvent(event: Map<String, Any?>) { _serverEvents.tryEmit(event) }

    private fun loadPlugin(context: Context, plugin: ElementPlugin) {
        val className = plugin.entryClass?.trim().orEmpty()
        val encoded = plugin.codeDexBase64?.trim().orEmpty()
        if (className.isBlank() || encoded.isBlank()) return
        runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            require(bytes.isNotEmpty()) { "Пустой DEX" }
            val dir = File(context.codeCacheDir, "element_plugins/" + plugin.id).apply { mkdirs() }
            val dex = File(dir, "plugin.dex").apply { writeBytes(bytes) }
            val optimized = File(dir, "optimized").apply { mkdirs() }
            val loader = DexClassLoader(dex.absolutePath, optimized.absolutePath, null, context.classLoader)
            val entry = loader.loadClass(className).getDeclaredConstructor().newInstance() as ElementPluginEntry
            entry.onLoad(createHost(context, plugin))
            loaded += Loaded(plugin, entry)
        }
    }

    private fun createHost(context: Context, plugin: ElementPlugin): ElementPluginHostV2 =
        object : ElementPluginHostV2 {
            override fun registerBottomNavigation(item: ElementPluginBottomNavItem): ElementPluginRegistration {
                registrations.removeAll { it.pluginId == plugin.id && it.itemId == item.id }
                registrations += ElementPluginNavItem(plugin.id, item.id, item.title, item.icon, item.iconBase64, item.badgeCount) { item.action.invoke() }
                publish()
                return ElementPluginRegistration(plugin.id, item.id)
            }
            override fun unregisterBottomNavigation(itemId: String) {
                registrations.removeAll { it.pluginId == plugin.id && it.itemId == itemId }
                publish()
            }
            override fun openPluginManager() { manager?.invoke() }
            override fun openPost(postId: Int): Boolean = postOpener?.invoke(postId) == true
            override fun openProfile(username: String): Boolean = profileOpener?.invoke(username) == true
            override fun showToast(message: String, long: Boolean) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context.applicationContext, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                }
            }
            override fun openUrl(url: String): Boolean = runCatching {
                context.applicationContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
            override fun currentUser(): ElementPluginUser = currentUserValue
            override fun appInfo(): ElementPluginAppInfo = appInfoValue
            override fun connectionState(): String =
                socket?.connectionState?.value?.name ?: ElementSocketClient.ConnectionState.Disconnected.name
            override fun setTheme(overrides: ElementPluginThemeOverrides) { ElementUiPalette.applyPluginOverrides(overrides) }
            override fun resetTheme() { ElementUiPalette.clearPluginOverrides() }
            override fun invalidateUi(reason: String) {}
            override fun storage(): ElementPluginStorage =
                ElementPluginStorage(context.getSharedPreferences("element_plugin_" + plugin.id, Context.MODE_PRIVATE))
            override val serverEvents: SharedFlow<Map<String, Any?>> get() = this@ElementPluginRuntime.serverEvents

            override suspend fun serverRequest(
                type: String,
                action: String,
                payload: Map<String, Any?>,
                timeoutMs: Long
            ): ElementPluginServerResponse {
                val client = socket ?: return ElementPluginServerResponse(false, null, "Socket client unavailable", emptyMap(), emptyMap())
                return runCatching {
                    val raw = client.sendRequest(
                        buildMap {
                            put("type", type)
                            put("action", action)
                            if (payload.isNotEmpty()) put("payload", payload)
                        },
                        timeoutMs.coerceIn(1_000, 120_000)
                    )
                    val status = raw["status"]?.toString()
                    val message = raw["message"]?.toString() ?: raw["error"]?.toString()
                    val ok = when (status?.lowercase()) {
                        null, "", "ok", "success", "200", "201" -> raw["error"] == null
                        else -> false
                    }
                    val data = (raw["data"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } ?: raw
                    ElementPluginServerResponse(ok, status, message, data, raw)
                }.getOrElse { ElementPluginServerResponse(false, null, it.message, emptyMap(), emptyMap()) }
            }
        }

    private fun unloadAll() {
        loaded.forEach { runCatching { it.entry.onUnload() } }
        loaded.clear()
        registrations.clear()
        ElementUiPalette.clearPluginOverrides()
        publish()
    }

    private fun publish() { _bottomNavigation.value = registrations.toList() }

    fun visualBalance(context: Context, fallback: Double): Double =
        ElementPluginStore(context).loadAll().firstOrNull()?.visualBalance ?: fallback

    fun visualHall(context: Context, currentDisplayName: String? = null, currentUsername: String? = null): VisualHallOverride? {
        val plugin = ElementPluginStore(context).loadAll().firstOrNull() ?: return null
        val balance = plugin.visualBalance ?: return null
        return VisualHallOverride(
            balance,
            plugin.visualHallRank ?: 1,
            if (plugin.useCurrentUser) currentDisplayName?.takeIf { it.isNotBlank() } ?: plugin.visualDisplayName ?: "Я" else plugin.visualDisplayName ?: "Я",
            if (plugin.useCurrentUser) currentUsername?.trim()?.removePrefix("@").takeIf { !it.isNullOrBlank() } ?: plugin.visualUsername ?: "" else plugin.visualUsername ?: ""
        )
    }
}

data class VisualHallOverride(val balance: Double, val rank: Int, val displayName: String, val username: String)
