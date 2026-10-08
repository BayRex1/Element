package example.element.plugin

import elemsocial.com.core.plugins.ElementPluginAction
import elemsocial.com.core.plugins.ElementPluginBottomNavItem
import elemsocial.com.core.plugins.ElementPluginEntryV2
import elemsocial.com.core.plugins.ElementPluginHostV2
import elemsocial.com.core.plugins.ElementPluginThemeOverrides
import elemsocial.com.core.plugins.sdk.ElementPluginServerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ExamplePlugin : ElementPluginEntryV2 {
    private var host: ElementPluginHostV2? = null
    private var job: Job? = null

    override fun onLoadV2(host: ElementPluginHostV2) {
        this.host = host

        host.registerBottomNavigation(
            ElementPluginBottomNavItem(
                id = "tools",
                title = "Tools",
                icon = "🛠️",
                action = ElementPluginAction {
                    host.showToast("Element Plugin API v2")
                    host.openPost(1)
                }
            )
        )

        host.setTheme(
            ElementPluginThemeOverrides(
                accentArgb = 0xFF7C4DFF.toInt()
            )
        )

        job = CoroutineScope(Dispatchers.Main.immediate).launch {
            val response = ElementPluginServerApi(host).loadPost(1)
            host.showToast(if (response.ok) "Post loaded" else "Server error")
        }
    }

    override fun onUnload() {
        job?.cancel()
        host?.resetTheme()
        host = null
    }
}
