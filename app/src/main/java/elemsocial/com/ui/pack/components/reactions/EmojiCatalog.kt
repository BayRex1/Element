package elemsocial.com.ui.pack.components.reactions

import android.content.Context
import org.json.JSONArray

data class EmojiItem(
    val unified: String,
    val emoji: String,
    val assetName: String? = null
)

object EmojiCatalog {

    private const val ASSET_CATALOG = "emoji_catalog.json"
    private const val ASSET_DIR = "emoji/apple"

    private var cached: List<EmojiItem>? = null

    fun load(context: Context): List<EmojiItem> {
        cached?.let { return it }
        val parsed = runCatching { loadFromAssets(context) }.getOrDefault(emptyList())
        val result = if (parsed.isNotEmpty()) parsed else fallback
        cached = result
        return result
    }

    fun assetDir(): String = ASSET_DIR

    private fun loadFromAssets(context: Context): List<EmojiItem> {
        val text = context.assets.open(ASSET_CATALOG).bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        val out = ArrayList<EmojiItem>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val unified = obj.optString("unified").takeIf { it.isNotBlank() } ?: continue
            val emoji = obj.optString("emoji").takeIf { it.isNotBlank() } ?: continue
            val assetName = obj.optString("asset").takeIf { it.isNotBlank() }
            out.add(EmojiItem(unified = unified, emoji = emoji, assetName = assetName))
        }
        return out
    }

    val fallback: List<EmojiItem> = listOf(
        EmojiItem("1f44d", "👍"),
        EmojiItem("2764_fe0f", "❤️"),
        EmojiItem("1f525", "🔥"),
        EmojiItem("1f602", "😂"),
        EmojiItem("1f62e", "😮"),
        EmojiItem("1f622", "😢"),
        EmojiItem("1f64f", "🙏"),
        EmojiItem("1f44f", "👏"),
        EmojiItem("1f389", "🎉"),
        EmojiItem("1f4af", "💯"),
        EmojiItem("1f60d", "😍"),
        EmojiItem("1f914", "🤔"),
        EmojiItem("1f44c", "👌"),
        EmojiItem("2705", "✅"),
        EmojiItem("1f621", "😡"),
        EmojiItem("1f923", "🤣"),
        EmojiItem("1f633", "😳"),
        EmojiItem("1f60e", "😎"),
        EmojiItem("1f643", "🙃"),
        EmojiItem("1f929", "🤩"),
        EmojiItem("1f92f", "🤯"),
        EmojiItem("1f634", "😴"),
        EmojiItem("1f924", "🤤"),
        EmojiItem("1f911", "🤑"),
        EmojiItem("1f92c", "🤬"),
        EmojiItem("1f44b", "👋"),
        EmojiItem("1f91d", "🤝"),
        EmojiItem("1f4aa", "💪"),
        EmojiItem("1f440", "👀"),
        EmojiItem("1f4a9", "💩"),
        EmojiItem("1f921", "🤡"),
        EmojiItem("1f480", "💀"),
        EmojiItem("1f47b", "👻"),
        EmojiItem("1f47d", "👽"),
        EmojiItem("1f916", "🤖")
    )
}
