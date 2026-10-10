package elemsocial.com.core.plugins

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dalvik.system.DexClassLoader
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.ui.pack.theme.ElementUiPalette
import elemsocial.com.domain.model.PostImageAsset
import java.io.File
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

const val ELEMENT_PLUGIN_API_VERSION = 4
const val ELEMENT_PLUGIN_SCRIPT_API_VERSION = 4

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
    private val pythonLoaded = CopyOnWriteArrayList<Pair<ElementPlugin, PyObject>>()
    private val pythonHooks = ConcurrentHashMap<String, CopyOnWriteArrayList<PyObject>>()
    private val pythonUiCallbacks = ConcurrentHashMap<String, PyObject>()
    private val _uiScreen = MutableStateFlow<ElementPluginUiScreen?>(null)
    val uiScreen: StateFlow<ElementPluginUiScreen?> = _uiScreen.asStateFlow()
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
    @Volatile private var appInfoValue = ElementPluginAppInfo(ELEMENT_PLUGIN_API_VERSION, "", "", 0)
    @Volatile private var appContext: Context? = null

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
        appContext = app
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
        if (!Python.isStarted()) Python.start(AndroidPlatform(app))
        ElementPluginStore(app).loadAll().forEach { loadPlugin(app, it) }
        publish()
        dispatchPythonHook("app.start", emptyMap())
        dispatchPythonHook("app.resume", emptyMap())
    }

    @Synchronized fun reload(context: Context) {
        val callback = manager ?: return
        attach(context, callback, socket, currentUserValue, postOpener ?: { false }, profileOpener ?: { false })
    }

    @Synchronized fun detach() {
        dispatchPythonHook("app.stop", emptyMap())
        unloadAll()
        manager = null
        postOpener = null
        profileOpener = null
        socket = null
        appContext = null
    }

    fun setUser(user: ElementPluginUser) { currentUserValue = user }
    fun emitAppEvent(event: String, payload: Map<String, Any?> = emptyMap()) {
        dispatchPythonHook(event, payload)
    }
    fun closeUiScreen() { _uiScreen.value = null }
    fun invokeUiCallback(callbackId: String, payloadJson: String = "{}") {
        pythonUiCallbacks[callbackId]?.let { callback ->
            runCatching {
                callback.call(Python.getInstance().getModule("json").callAttr("loads", payloadJson))
            }.onFailure {
                appContext?.let { ctx ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(ctx, "Plugin UI error: " + (it.message ?: "error"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    fun openPluginSettings(pluginId: String): Boolean {
        val pair = pythonLoaded.firstOrNull { it.first.id == pluginId } ?: return false
        return runCatching {
            val result = Python.getInstance().getModule("element_plugin_runtime")
                .callAttr("open_legacy_settings", pair.second)
            result.toString().equals("True", ignoreCase = true)
        }.onFailure {
            appContext?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "Настройки плагина: " + (it.message ?: "ошибка"), Toast.LENGTH_LONG).show()
                }
            }
        }.getOrDefault(false)
    }
    fun emitServerEvent(event: Map<String, Any?>) {
        _serverEvents.tryEmit(event)
        dispatchPythonHook("server.event", event)
        event["type"]?.toString()?.takeIf { it.isNotBlank() }?.let { dispatchPythonHook(it, event) }
        event["action"]?.toString()?.takeIf { it.isNotBlank() }?.let { dispatchPythonHook(it, event) }
    }

    private fun loadPlugin(context: Context, plugin: ElementPlugin) {
        val className = plugin.entryClass?.trim().orEmpty()
        val encoded = plugin.codeDexBase64?.trim().orEmpty()
        if (className.isBlank() || encoded.isBlank()) {
            loadScriptPlugin(context, plugin)
            return
        }
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

    private fun loadScriptPlugin(context: Context, plugin: ElementPlugin) {
        runCatching {
            val file = File(context.filesDir, "plugins/" + plugin.fileName)
            require(file.isFile) { "Файл плагина не найден" }
            if (file.readText(Charsets.UTF_8).trimStart().startsWith("{")) return@runCatching
            val bridge = PythonPluginBridge(context, plugin)
            val instance = Python.getInstance().getModule("element_plugin_runtime")
                .callAttr("load_plugin", file.absolutePath, bridge)
            pythonLoaded += plugin to instance
        }.onFailure {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Плагин «" + plugin.name + "»: " + (it.message ?: "ошибка загрузки"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private class PythonPluginBridge(
        private val context: Context,
        private val plugin: ElementPlugin
    ) {
        fun show_toast(message: String, long: Boolean) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            }
        }
        fun open_post(postId: Int): Boolean = postOpener?.invoke(postId) == true
        fun open_profile(username: String): Boolean = profileOpener?.invoke(username) == true
        fun open_url(url: String): Boolean = runCatching {
            context.applicationContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
        fun current_user_json(): String = JSONObject(mapOf(
            "id" to currentUserValue.id, "name" to currentUserValue.name,
            "username" to currentUserValue.username, "email" to currentUserValue.email
        )).toString()
        fun app_info_json(): String = JSONObject(mapOf(
            "api_version" to ELEMENT_PLUGIN_SCRIPT_API_VERSION,
            "application_id" to appInfoValue.applicationId,
            "version_name" to appInfoValue.versionName, "version_code" to appInfoValue.versionCode
        )).toString()
        fun connection_state(): String = socket?.connectionState?.value?.name
            ?: ElementSocketClient.ConnectionState.Disconnected.name
        private fun prefs() = context.getSharedPreferences("element_plugin_" + plugin.id, Context.MODE_PRIVATE)
        fun storage_get(key: String): String? = prefs().getString(key, null)
        fun storage_put(key: String, value: String) { prefs().edit().putString(key, value).apply() }
        fun storage_remove(key: String) { prefs().edit().remove(key).apply() }
        fun storage_clear() { prefs().edit().clear().apply() }
        fun register_bottom_button(itemId: String, title: String, icon: String, callback: PyObject, badge: Int?): Map<String, Any?> {
            registrations.removeAll { it.pluginId == plugin.id && it.itemId == itemId }
            registrations += ElementPluginNavItem(plugin.id, itemId, title, icon, null, badge) {
                runCatching { callback.call() }.onFailure { show_toast("Plugin error: " + (it.message ?: "unknown"), true) }
            }
            publish()
            return mapOf("plugin_id" to plugin.id, "item_id" to itemId)
        }
        fun unregister_bottom_button(itemId: String) {
            registrations.removeAll { it.pluginId == plugin.id && it.itemId == itemId }; publish()
        }
        fun register_hook(event: String, callback: PyObject) {
            pythonHooks.getOrPut(event) { CopyOnWriteArrayList() }.add(callback)
        }
        fun unregister_hook(event: String, callback: PyObject) { pythonHooks[event]?.removeAll { it === callback } }
        fun register_ui_callback(callbackId: String, callback: PyObject): String {
            val key = plugin.id + ":" + callbackId
            pythonUiCallbacks[key] = callback
            return key
        }
        fun show_ui_screen_json(json: String) {
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return
            _uiScreen.value = parseUiScreen(plugin.id, root)
        }
        fun close_ui_screen() {
            if (_uiScreen.value?.pluginId == plugin.id) _uiScreen.value = null
        }
        fun set_theme_json(json: String) {
            val overrides = runCatching { JSONObject(json) }.getOrNull() ?: return
            fun color(key: String): Int? {
                val raw = overrides.optString(key, "").trim().removePrefix("#")
                if (raw.isBlank()) return null
                return runCatching {
                    val value = raw.toLong(16).toInt()
                    if (raw.length <= 6) 0xFF000000.toInt() or value else value
                }.getOrNull()
            }
            ElementUiPalette.applyPluginOverrides(ElementPluginThemeOverrides(
                accentArgb = color("accent"), bodyArgb = color("body"), blockArgb = color("block"),
                blockSoftArgb = color("block_soft"), textPrimaryArgb = color("text_primary"),
                textSecondaryArgb = color("text_secondary"), errorArgb = color("error"),
                successArgb = color("success"), infoArgb = color("info")
            ))
        }
        fun reset_theme() { ElementUiPalette.clearPluginOverrides() }
        fun server_request(type: String, action: String, payloadJson: String, timeoutMs: Int): String {
            val payload = jsonObjectToMap(runCatching { JSONObject(payloadJson) }.getOrNull())
            val response = runBlocking(Dispatchers.IO) {
                val client = socket ?: return@runBlocking ElementPluginServerResponse(false, null, "Socket client unavailable", emptyMap(), emptyMap())
                runCatching {
                    val raw = client.sendRequest(buildMap {
                        put("type", type)
                        put("action", action)
                        if (payload.isNotEmpty()) put("payload", payload)
                    }, timeoutMs.toLong().coerceIn(1_000, 120_000))
                    val status = raw["status"]?.toString()
                    val message = raw["message"]?.toString() ?: raw["error"]?.toString()
                    val ok = when (status?.lowercase()) { null, "", "ok", "success", "200", "201" -> raw["error"] == null; else -> false }
                    val data = (raw["data"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } ?: raw
                    ElementPluginServerResponse(ok, status, message, data, raw)
                }.getOrElse { ElementPluginServerResponse(false, null, it.message, emptyMap(), emptyMap()) }
            }
            return JSONObject(mapOf("ok" to response.ok, "status" to response.status, "message" to response.message, "data" to response.data, "raw" to response.raw)).toString()
        }

        private fun jsonObjectToMap(json: JSONObject?): Map<String, Any?> {
            if (json == null) return emptyMap()
            return buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = json.opt(key)
                    put(key, when (value) {
                        is JSONObject -> jsonObjectToMap(value)
                        JSONObject.NULL -> null
                        else -> value
                    })
                }
            }
        }
    }

    private fun parseUiScreen(pluginId: String, root: JSONObject): ElementPluginUiScreen {
        fun node(value: JSONObject): ElementPluginUiNode {
            val children = mutableListOf<ElementPluginUiNode>()
            value.optJSONArray("children")?.let { array ->
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { children += node(it) }
                }
            }
            return ElementPluginUiNode(
                type = value.optString("type", "text"),
                text = value.optString("text", ""),
                secondary = value.optString("secondary", ""),
                callbackId = value.optString("callback_id").takeIf { it.isNotBlank() },
                value = value.optString("value", ""),
                checked = value.optBoolean("checked", false),
                enabled = value.optBoolean("enabled", true),
                children = children,
                options = value.optJSONArray("options")?.let { array ->
                    (0 until array.length()).map { index -> array.optString(index) }
                } ?: emptyList()
            )
        }
        val nodes = mutableListOf<ElementPluginUiNode>()
        root.optJSONArray("nodes")?.let { array ->
            for (i in 0 until array.length()) array.optJSONObject(i)?.let { nodes += node(it) }
        }
        return ElementPluginUiScreen(
            pluginId = pluginId,
            screenId = root.optString("id", "screen"),
            title = root.optString("title", pluginId),
            nodes = nodes
        )
    }

    private fun dispatchPythonHook(event: String, payload: Map<String, Any?>) {
        pythonHooks[event]?.toList()?.forEach { callback ->
            runCatching { callback.call(Python.getInstance().getModule("json").callAttr("loads", JSONObject(payload).toString())) }.onFailure {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(appContext, "Plugin hook " + event + ": " + (it.message ?: "error"), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val legacyEvent = when (event) {
            "app.start" -> "START"
            "app.stop" -> "STOP"
            "app.pause" -> "PAUSE"
            "app.resume" -> "RESUME"
            else -> null
        }
        if (legacyEvent != null) {
            pythonLoaded.toList().forEach { (_, instance) ->
                runCatching { instance.callAttr("on_app_event", legacyEvent) }.onFailure {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(appContext, "Plugin lifecycle " + event + ": " + (it.message ?: "error"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        when (event) {
            "send_message_hook" -> pythonLoaded.toList().forEach { (_, instance) ->
                runCatching { instance.callAttr("on_send_message_hook", payload["account"] ?: 0, payload["params"] ?: emptyMap<String, Any?>()) }
            }
            "request.before" -> pythonLoaded.toList().forEach { (_, instance) ->
                runCatching { instance.callAttr("pre_request_hook", payload["request_name"] ?: "", payload["account"] ?: 0, payload["request"] ?: emptyMap<String, Any?>()) }
            }
            "request.after" -> pythonLoaded.toList().forEach { (_, instance) ->
                runCatching { instance.callAttr("post_request_hook", payload["request_name"] ?: "", payload["account"] ?: 0, payload["response"] ?: emptyMap<String, Any?>(), payload["error"]) }
            }
            "update" -> pythonLoaded.toList().forEach { (_, instance) ->
                runCatching { instance.callAttr("on_update_hook", payload["update_name"] ?: "", payload["account"] ?: 0, payload["update"] ?: emptyMap<String, Any?>()) }
            }
            "updates" -> pythonLoaded.toList().forEach { (_, instance) ->
                runCatching { instance.callAttr("on_updates_hook", payload["container_name"] ?: "", payload["account"] ?: 0, payload["updates"] ?: emptyMap<String, Any?>()) }
            }
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
            override fun openPost(postId: Int): Boolean {
                val result = postOpener?.invoke(postId) == true
                if (result) dispatchPythonHook("post.opened", mapOf("post_id" to postId))
                return result
            }
            override fun openProfile(username: String): Boolean {
                val result = profileOpener?.invoke(username) == true
                if (result) dispatchPythonHook("profile.opened", mapOf("username" to username))
                return result
            }
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
        pythonLoaded.forEach { (_, instance) ->
            runCatching {
                Python.getInstance().getModule("element_plugin_runtime").callAttr("unload_plugin", instance)
            }
        }
        pythonLoaded.clear()
        pythonHooks.clear()
        pythonUiCallbacks.clear()
        _uiScreen.value = null
        registrations.clear()
        ElementUiPalette.clearPluginOverrides()
        publish()
    }

    private fun publish() { _bottomNavigation.value = registrations.toList() }

    fun visualBalance(context: Context, fallback: Double): Double =
        ElementPluginStore(context).loadAll().firstOrNull()?.visualBalance ?: fallback

    fun visualHall(context: Context, currentDisplayName: String? = null, currentUsername: String? = null, currentAvatar: PostImageAsset? = null): VisualHallOverride? {
        val plugin = ElementPluginStore(context).loadAll().firstOrNull() ?: return null
        val balance = plugin.visualBalance ?: return null
        return VisualHallOverride(
            balance,
            plugin.visualHallRank ?: 1,
            if (plugin.useCurrentUser) currentDisplayName?.takeIf { it.isNotBlank() } ?: plugin.visualDisplayName ?: "Я" else plugin.visualDisplayName ?: "Я",
            if (plugin.useCurrentUser) currentUsername?.trim()?.removePrefix("@").takeIf { !it.isNullOrBlank() } ?: plugin.visualUsername ?: "" else plugin.visualUsername ?: "",
            if (plugin.useCurrentUser) currentAvatar else null
        )
    }
}

data class VisualHallOverride(
    val balance: Double,
    val rank: Int,
    val displayName: String,
    val username: String,
    val avatar: PostImageAsset? = null
)
