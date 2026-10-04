package elemsocial.com.core.cache

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MusicCacheIndexStore(
    context: Context
) {
    data class Entry(
        val fileId: Int,
        val cacheKey: String,
        val mime: String,
        val size: Long
    )

    private val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    private val lock = Any()

    fun read(fileId: Int): Entry? = synchronized(lock) {
        readAll()[fileId]
    }

    fun write(entry: Entry) = synchronized(lock) {
        val all = readAll().toMutableMap()
        all[entry.fileId] = entry
        persist(all.values.toList())
    }

    fun remove(fileId: Int) = synchronized(lock) {
        val all = readAll().toMutableMap()
        all.remove(fileId)
        persist(all.values.toList())
    }

    fun clear() = synchronized(lock) {
        prefs.edit().remove(KeyEntries).apply()
    }

    private fun readAll(): Map<Int, Entry> {
        val raw = prefs.getString(KeyEntries, null)?.trim().orEmpty()
        if (raw.isBlank()) return emptyMap()

        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val fileId = item.optInt("fileId", -1)
                    val cacheKey = item.optString("cacheKey")
                    val mime = item.optString("mime")
                    val size = item.optLong("size", -1L)
                    if (fileId <= 0 || cacheKey.isBlank() || mime.isBlank() || size < 0L) continue
                    put(
                        fileId,
                        Entry(
                            fileId = fileId,
                            cacheKey = cacheKey,
                            mime = mime,
                            size = size
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun persist(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("fileId", entry.fileId)
                    .put("cacheKey", entry.cacheKey)
                    .put("mime", entry.mime)
                    .put("size", entry.size)
            )
        }
        prefs.edit().putString(KeyEntries, array.toString()).apply()
    }

    private companion object {
        const val PrefsName = "element_music_cache_index"
        const val KeyEntries = "entries"
    }
}
