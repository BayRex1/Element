package elemsocial.com.core.plugins

data class ElementPlugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val icon: String,
    val fileName: String,
    val settings: Map<String, String> = emptyMap(),
    val visualBalance: Double? = null,
    val visualHallRank: Int? = null,
    val visualDisplayName: String? = null,
    val visualUsername: String? = null
)

object ElementPluginRuntime {
    fun activePlugin(context: android.content.Context): ElementPlugin? =
        ElementPluginStore(context).loadAll().firstOrNull()

    fun visualBalance(context: android.content.Context, fallback: Double): Double {
        return activePlugin(context)?.visualBalance ?: fallback
    }

    fun visualHall(context: android.content.Context): VisualHallOverride? {
        val plugin = activePlugin(context) ?: return null
        val balance = plugin.visualBalance ?: return null
        return VisualHallOverride(
            balance = balance,
            rank = plugin.visualHallRank ?: 1,
            displayName = plugin.visualDisplayName ?: "Я",
            username = plugin.visualUsername ?: "me"
        )
    }
}

data class VisualHallOverride(
    val balance: Double,
    val rank: Int,
    val displayName: String,
    val username: String
)
