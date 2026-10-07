package elemsocial.com.core.plugins

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
    val useCurrentUser: Boolean = true
)

object ElementPluginRuntime {
    fun activePlugin(context: android.content.Context): ElementPlugin? =
        ElementPluginStore(context).loadAll().firstOrNull()

    fun visualBalance(context: android.content.Context, fallback: Double): Double {
        return activePlugin(context)?.visualBalance ?: fallback
    }

    fun visualHall(
        context: android.content.Context,
        currentDisplayName: String? = null,
        currentUsername: String? = null
    ): VisualHallOverride? {
        val plugin = activePlugin(context) ?: return null
        val balance = plugin.visualBalance ?: return null
        return VisualHallOverride(
            balance = balance,
            rank = plugin.visualHallRank ?: 1,
            displayName = if (plugin.useCurrentUser) currentDisplayName?.takeIf { it.isNotBlank() } ?: plugin.visualDisplayName ?: "Я" else plugin.visualDisplayName ?: "Я",
            username = if (plugin.useCurrentUser) currentUsername?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() } ?: plugin.visualUsername ?: "" else plugin.visualUsername ?: ""
        )
    }
}

data class VisualHallOverride(
    val balance: Double,
    val rank: Int,
    val displayName: String,
    val username: String
)
