package elemsocial.com.core.plugins

import android.content.Context
import android.util.Base64
import dalvik.system.DexClassLoader
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/** Entry point implemented by a native Element plugin inside its DEX. */
interface ElementPluginEntry {
    fun onLoad(host: ElementPluginHost)
    fun onUnload() {}
}

/** Stable host API exposed to plugin code. Keep this API small and backwards compatible. */
interface ElementPluginHost {
    fun registerBottomNavigation(item: ElementPluginBottomNavItem): ElementPluginRegistration
    fun openPluginManager()
}

fun interface ElementPluginAction {
    fun invoke()
}

data class ElementPluginBottomNavItem(
    val id: String,
    val title: String,
    val icon: String = "🧩",
    val iconBase64: String? = null,
    val action: ElementPluginAction
)

data class ElementPluginRegistration(
    val pluginId: String,
    val itemId: String
)

data class ElementPluginNavItem(
    val pluginId: String,
    val itemId: String,
    val title: String,
    val icon: String,
    val iconBase64: String? = null,
    val action: () -> Unit
)

/**
 * Native plugin runtime. The host does not know individual plugin IDs or features.
 * A plugin DEX supplies ElementPluginEntry and registers its own UI/actions through
 * ElementPluginHost.
 */
object ElementPluginRuntime {
    private data class LoadedPlugin(
        val plugin: ElementPlugin,
        val entry: ElementPluginEntry
    )

    private val loaded = CopyOnWriteArrayList<LoadedPlugin>()
    private val registrations = CopyOnWriteArrayList<ElementPluginNavItem>()
    private val _bottomNavigation = MutableStateFlow<List<ElementPluginNavItem>>(emptyList())
    val bottomNavigation: StateFlow<List<ElementPluginNavItem>> = _bottomNavigation.asStateFlow()

    @Volatile
    private var openPluginManager: (() -> Unit)? = null

    @Synchronized
    fun attach(context: Context, onOpenPluginManager: () -> Unit) {
        unloadAll()
        openPluginManager = onOpenPluginManager
        val store = ElementPluginStore(context)
        store.loadAll().forEach { plugin ->
            loadPlugin(context, plugin)
        }
        publish()
    }

    @Synchronized
    fun reload(context: Context) {
        val callback = openPluginManager ?: return
        attach(context, callback)
    }

    @Synchronized
    fun detach() {
        unloadAll()
        openPluginManager = null
    }

    private fun loadPlugin(context: Context, plugin: ElementPlugin) {
        val className = plugin.entryClass?.trim().orEmpty()
        val encodedDex = plugin.codeDexBase64?.trim().orEmpty()
        if (className.isBlank() || encodedDex.isBlank()) return

        runCatching {
            val dexBytes = Base64.decode(encodedDex, Base64.DEFAULT)
            require(dexBytes.isNotEmpty()) { "Пустой DEX" }
            val pluginDir = File(context.codeCacheDir, "element_plugins/${plugin.id}").apply { mkdirs() }
            val dexFile = File(pluginDir, "plugin.dex")
            dexFile.writeBytes(dexBytes)
            val optimized = File(pluginDir, "optimized").apply { mkdirs() }
            val loader = DexClassLoader(
                dexFile.absolutePath,
                optimized.absolutePath,
                context.classLoader
            )
            val entry = loader.loadClass(className).getDeclaredConstructor().newInstance() as ElementPluginEntry
            val pluginHost = object : ElementPluginHost {
                override fun registerBottomNavigation(item: ElementPluginBottomNavItem): ElementPluginRegistration {
                    val registration = ElementPluginRegistration(plugin.id, item.id)
                    registrations.removeAll { it.pluginId == plugin.id && it.itemId == item.id }
                    registrations += ElementPluginNavItem(
                        pluginId = plugin.id,
                        itemId = item.id,
                        title = item.title,
                        icon = item.icon,
                        iconBase64 = item.iconBase64,
                        action = { item.action.invoke() }
                    )
                    publish()
                    return registration
                }

                override fun openPluginManager() {
                    openPluginManager?.invoke()
                }
            }
            entry.onLoad(pluginHost)
            loaded += LoadedPlugin(plugin, entry)
        }.onFailure {
            // Broken third-party plugins are isolated: one bad plugin must not kill Element.
        }
    }

    private fun unloadAll() {
        loaded.forEach { runCatching { it.entry.onUnload() } }
        loaded.clear()
        registrations.clear()
        publish()
    }

    private fun publish() {
        _bottomNavigation.value = registrations.toList()
    }

    fun visualBalance(context: Context, fallback: Double): Double {
        return ElementPluginStore(context).loadAll().firstOrNull()?.visualBalance ?: fallback
    }

    fun visualHall(
        context: Context,
        currentDisplayName: String? = null,
        currentUsername: String? = null
    ): VisualHallOverride? {
        val plugin = ElementPluginStore(context).loadAll().firstOrNull() ?: return null
        val balance = plugin.visualBalance ?: return null
        return VisualHallOverride(
            balance = balance,
            rank = plugin.visualHallRank ?: 1,
            displayName = if (plugin.useCurrentUser) currentDisplayName?.takeIf { it.isNotBlank() } ?: plugin.visualDisplayName ?: "Я" else plugin.visualDisplayName ?: "Я",
            username = if (plugin.useCurrentUser) currentUsername?.trim()?.removePrefix("@").takeIf { !it.isNullOrBlank() } ?: plugin.visualUsername ?: "" else plugin.visualUsername ?: ""
        )
    }
}

data class VisualHallOverride(
    val balance: Double,
    val rank: Int,
    val displayName: String,
    val username: String
)
