package elemsocial.com.core.plugins

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

data class PluginManifest(val id: String, val name: String, val version: String = "1.0.0", val author: String = "", val description: String = "") {
    init {
        require(id.matches(Regex("[a-zA-Z0-9_.-]{2,80}")))
        require(name.isNotBlank())
    }
}

sealed interface ElementPluginEvent {
    data object AppStarted : ElementPluginEvent
    data class ThemeChanged(val isDark: Boolean) : ElementPluginEvent
    data class SessionChanged(val accountId: Int?, val isAuthorized: Boolean) : ElementPluginEvent
    data class ScreenOpened(val screenId: String) : ElementPluginEvent
    data class PostAction(val action: String, val postId: String? = null) : ElementPluginEvent
}

data class BeforePublishEvent(val text: String, val attachments: List<String> = emptyList(), var cancelled: Boolean = false, var cancelReason: String? = null, var replacementText: String = text)
fun interface PluginSubscription { fun cancel() }

interface ElementPlugin {
    val manifest: PluginManifest
    fun onLoad(context: PluginContext) {}
    fun onEnable() {}
    fun onDisable() {}
    fun onUnload() {}
}

class PluginContext internal constructor(val appContext: Context, private val hooks: PluginHookBus) {
    fun subscribe(listener: (ElementPluginEvent) -> Unit): PluginSubscription = hooks.subscribe(listener)
    fun onBeforePublish(listener: (BeforePublishEvent) -> Unit): PluginSubscription = hooks.onBeforePublish(listener)
    fun emit(event: ElementPluginEvent) = hooks.emit(event)
    fun overrideColor(key: String, argb: Long?) = hooks.setColorOverride(key, argb)
    fun colorOverride(key: String): Long? = hooks.getColorOverride(key)
}

class PluginHookBus {
    private val listeners = CopyOnWriteArrayList<(ElementPluginEvent) -> Unit>()
    private val publishListeners = CopyOnWriteArrayList<(BeforePublishEvent) -> Unit>()
    private val colorOverrides = linkedMapOf<String, Long>()

    fun subscribe(listener: (ElementPluginEvent) -> Unit): PluginSubscription {
        listeners += listener
        return PluginSubscription { listeners -= listener }
    }
    fun onBeforePublish(listener: (BeforePublishEvent) -> Unit): PluginSubscription {
        publishListeners += listener
        return PluginSubscription { publishListeners -= listener }
    }
    fun emit(event: ElementPluginEvent) { listeners.forEach { runCatching { it(event) } } }
    fun interceptBeforePublish(event: BeforePublishEvent): BeforePublishEvent {
        publishListeners.forEach { if (!event.cancelled) runCatching { it(event) } }
        return event
    }
    @Synchronized fun setColorOverride(key: String, argb: Long?) {
        require(key.matches(Regex("[a-zA-Z0-9_.-]{1,120}")))
        if (argb == null) colorOverrides.remove(key) else {
            require(argb in 0x00000000L..0xFFFFFFFFL)
            colorOverrides[key] = argb
        }
    }
    @Synchronized fun getColorOverride(key: String): Long? = colorOverrides[key]
    @Synchronized fun clearColorOverrides() = colorOverrides.clear()
}

class ElementPluginManager(context: Context) {
    private val appContext = context.applicationContext
    private val hooks = PluginHookBus()
    private val plugins = linkedMapOf<String, ElementPlugin>()
    private val enabledIds = linkedSetOf<String>()
    val context = PluginContext(appContext, hooks)

    @Synchronized fun register(plugin: ElementPlugin) {
        val id = plugin.manifest.id
        require(id !in plugins) { "Plugin already registered: $id" }
        plugins[id] = plugin
        try { plugin.onLoad(context) } catch (error: Throwable) {
            plugins.remove(id)
            throw IllegalStateException("Failed to load plugin $id", error)
        }
    }
    @Synchronized fun enable(id: String) {
        val plugin = requireNotNull(plugins[id]) { "Unknown plugin: $id" }
        if (enabledIds.add(id)) try { plugin.onEnable() } catch (error: Throwable) {
            enabledIds.remove(id); throw error
        }
    }
    @Synchronized fun disable(id: String) {
        val plugin = plugins[id] ?: return
        if (enabledIds.remove(id)) plugin.onDisable()
    }
    @Synchronized fun unregister(id: String) {
        val plugin = plugins[id] ?: return
        disable(id); runCatching { plugin.onUnload() }; plugins.remove(id)
    }
    @Synchronized fun manifests(): List<PluginManifest> = plugins.values.map { it.manifest }
    @Synchronized fun isEnabled(id: String): Boolean = id in enabledIds
    fun emit(event: ElementPluginEvent) = hooks.emit(event)
    fun beforePublish(text: String, attachments: List<String> = emptyList()): BeforePublishEvent = hooks.interceptBeforePublish(BeforePublishEvent(text, attachments))
}
