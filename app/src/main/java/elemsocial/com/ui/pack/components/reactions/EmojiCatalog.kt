package elemsocial.com.ui.pack.components.reactions

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class EmojiItem(
    val unified: String,
    val emoji: String
)

object EmojiCatalog {

    private const val ASSET_CATALOG = "post_reaction_emoji_catalog.json"
    private const val ASSET_DIR = "emoji/apple"

    private var cached: List<EmojiItem>? = null
    private var cachedMap: Map<String, String>? = null

    fun load(context: Context): List<EmojiItem> {
        cached?.let { return it }
        val parsed = runCatching { loadFromAssets(context) }.getOrDefault(emptyList())
        val result = if (parsed.isNotEmpty()) parsed else fallback
        cached = result
        cachedMap = result.associate { it.unified.uppercase() to it.emoji }
        return result
    }

    fun emojiFor(context: Context, unified: String): String? {
        if (cachedMap == null) load(context)
        return cachedMap?.get(unified.trim().uppercase())
            ?: cachedMap?.get(unified.trim().uppercase().replace("-", "_"))
    }

    fun assetDir(): String = ASSET_DIR

    private fun loadFromAssets(context: Context): List<EmojiItem> {
        val text = context.assets.open(ASSET_CATALOG).bufferedReader().use { it.readText() }
        val root = JSONArray(text)
        val out = ArrayList<EmojiItem>()

        for (i in 0 until root.length()) {
            val groupObj: JSONObject = root.optJSONObject(i) ?: continue
            val groupKeys = groupObj.keys()
            while (groupKeys.hasNext()) {
                val categoryKey = groupKeys.next()
                val items = groupObj.optJSONArray(categoryKey) ?: continue
                for (j in 0 until items.length()) {
                    val obj = items.optJSONObject(j) ?: continue
                    val unified = obj.optString("unified").takeIf { it.isNotBlank() } ?: continue
                    val emoji = obj.optString("emoji").takeIf { it.isNotBlank() } ?: continue
                    out.add(EmojiItem(unified = unified, emoji = emoji))
                }
            }
        }

        return out
    }

    private val fallback: List<EmojiItem> = listOf(
        EmojiItem("1F44D", "👍"),
        EmojiItem("2764-FE0F", "❤️"),
        EmojiItem("1F525", "🔥"),
        EmojiItem("1F602", "😂"),
        EmojiItem("1F62E", "😮"),
        EmojiItem("1F622", "😢"),
        EmojiItem("1F64F", "🙏"),
        EmojiItem("1F44F", "👏"),
        EmojiItem("1F389", "🎉"),
        EmojiItem("1F4AF", "💯"),
        EmojiItem("1F60D", "😍"),
        EmojiItem("1F914", "🤔"),
        EmojiItem("1F44C", "👌"),
        EmojiItem("2705", "✅"),
        EmojiItem("1F621", "😡"),
        EmojiItem("1F923", "🤣"),
        EmojiItem("1F633", "😳"),
        EmojiItem("1F60E", "😎"),
        EmojiItem("1F643", "🙃"),
        EmojiItem("1F929", "🤩")
    )
}
