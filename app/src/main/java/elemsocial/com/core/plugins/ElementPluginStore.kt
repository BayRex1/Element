package elemsocial.com.core.plugins

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

class ElementPluginStore(private val context: Context) {
    private val dir: File by lazy { File(context.filesDir, "plugins").apply { mkdirs() } }

    fun loadAll(): List<ElementPlugin> = dir.listFiles { file -> file.isFile && file.extension.lowercase(Locale.ROOT) == "plugin" }
        ?.mapNotNull { parse(it) }
        ?.sortedBy { it.name.lowercase(Locale.ROOT) }
        ?: emptyList()

    fun delete(plugin: ElementPlugin): Boolean = File(dir, plugin.fileName).delete()

    fun savePlugin(bytes: ByteArray, originalName: String): Result<ElementPlugin> = runCatching {
        require(originalName.lowercase(Locale.ROOT).endsWith(".plugin")) { "Выберите файл с расширением .plugin" }
        require(bytes.isNotEmpty()) { "Файл плагина пуст" }
        val text = String(bytes, Charsets.UTF_8)
        val safeName = if (text.trimStart().startsWith("{")) {
            val json = JSONObject(text)
            val id = json.getString("id").trim()
            require(id.matches(Regex("[A-Za-z0-9_.-]+"))) { "Некорректный id плагина" }
            "$id.plugin"
        } else {
            val id = scriptString(text, "__id__") ?: error("Не указан __id__")
            require(id.matches(Regex("[A-Za-z0-9_.-]+"))) { "Некорректный __id__" }
            "$id.plugin"
        }
        val target = File(dir, safeName)
        target.writeBytes(bytes)
        parse(target) ?: error("Некорректный файл плагина")
    }

    private fun parse(file: File): ElementPlugin? = runCatching {
        val text = file.readText(Charsets.UTF_8)
        if (text.trimStart().startsWith("{")) parseJson(file, text) else parseScript(file, text)
    }.getOrNull()

    private fun parseJson(file: File, text: String): ElementPlugin {
        val root = JSONObject(text)
        val settingsJson = root.optJSONObject("settings")
        val settings = buildMap {
            settingsJson?.keys()?.forEach { key -> put(key, settingsJson.optString(key)) }
        }
        val visual = root.optJSONObject("visual_overrides")
        return ElementPlugin(
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
            entryClass = root.optString("entry_class").takeIf { it.isNotBlank() },
            codeDexBase64 = root.optString("code_dex_base64").takeIf { it.isNotBlank() }
        )
    }

    private fun parseScript(file: File, text: String): ElementPlugin {
        val id = scriptString(text, "__id__") ?: error("Не указан __id__")
        require(id.matches(Regex("[A-Za-z0-9_.-]+"))) { "Некорректный __id__" }
        return ElementPlugin(
            id = id,
            name = scriptString(text, "__name__") ?: id,
            description = scriptString(text, "__description__") ?: "",
            version = scriptString(text, "__version__") ?: "1.0.0",
            author = scriptString(text, "__author__") ?: "Unknown",
            icon = scriptString(text, "__icon__") ?: "🧩",
            fileName = file.name
        )
    }

    private fun scriptString(text: String, name: String): String? {
        val pattern = Pattern.compile(
            """(?m)^\s*${Pattern.quote(name)}\s*=\s*(["'])(.*?)\1\s*(?:#.*)?$"""
        )
        return pattern.matcher(text).let { if (it.find()) it.group(2)?.trim() else null }
    }
}
