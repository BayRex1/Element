package elemsocial.com.core.plugins

import android.content.Context
import org.json.JSONObject
import java.io.File

class ElementPluginStore(private val context: Context) {
    private val dir: File by lazy { File(context.filesDir, "plugins").apply { mkdirs() } }

    fun loadAll(): List<ElementPlugin> = dir.listFiles { file -> file.isFile && file.extension == "plugin" }
        ?.mapNotNull { parse(it) }
        ?.sortedBy { it.name.lowercase() }
        ?: emptyList()

    fun delete(plugin: ElementPlugin): Boolean {
        return File(dir, plugin.fileName).delete()
    }

    fun savePlugin(bytes: ByteArray, originalName: String): Result<ElementPlugin> = runCatching {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        val id = json.getString("id").trim()
        require(id.matches(Regex("[A-Za-z0-9_.-]+"))) { "Некорректный id плагина" }
        val safeName = id + ".plugin"
        val target = File(dir, safeName)
        target.writeBytes(bytes)
        parse(target) ?: error("Некорректный файл плагина")
    }

    private fun parse(file: File): ElementPlugin? = runCatching {
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val settingsJson = root.optJSONObject("settings")
        val settings = buildMap {
            settingsJson?.keys()?.forEach { key -> put(key, settingsJson.optString(key)) }
        }
        val visual = root.optJSONObject("visual_overrides")
        ElementPlugin(
            id = root.getString("id"),
            name = root.optString("name", root.getString("id")),
            description = root.optString("description", ""),
            version = root.optString("version", "1.0"),
            author = root.optString("author", "Unknown"),
            icon = root.optString("icon", "🧩").takeIf { it != "[object]" } ?: "🧩",
            iconBase64 = root.optJSONObject("icon_image")?.optString("data_base64")?.takeIf { it.isNotBlank() },
            iconMimeType = root.optJSONObject("icon_image")?.optString("mime", "image/png"),
            fileName = file.name,
            settings = settings,
            visualBalance = visual?.optDouble("balance", Double.NaN)?.takeUnless { it.isNaN() },
            visualHallRank = visual?.optInt("hall_rank", 1),
            visualDisplayName = visual?.optString("display_name")?.takeIf { it.isNotBlank() },
            visualUsername = visual?.optString("username")?.takeIf { it.isNotBlank() },
            useCurrentUser = visual?.optBoolean("use_current_user", true) ?: true,
            entryClass = root.optString("entry_class")?.takeIf { it.isNotBlank() },
            codeDexBase64 = root.optString("code_dex_base64")?.takeIf { it.isNotBlank() }
        )
    }.getOrNull()
}
