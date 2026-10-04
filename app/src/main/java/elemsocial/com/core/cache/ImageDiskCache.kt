package elemsocial.com.core.cache

import android.content.Context
import java.io.File
import java.security.MessageDigest

class ImageDiskCache(
    context: Context
) {
    enum class StorageCategory {
        Avatars,
        Messenger,
        Covers,
        Music,
        Posts,
        Comments
    }

    data class StorageStats(
        val totalBytes: Long,
        val categoryBytes: Map<StorageCategory, Long>
    )

    data class StorageClearResult(
        val filesDeleted: Int = 0,
        val bytesDeleted: Long = 0L
    )

    private val appCacheDir = context.cacheDir
    private val cacheDir = File(appCacheDir, "element_image_cache")
    private val musicProgressiveDir = File(appCacheDir, "element_music_progressive")
    private val musicCacheIndexStore = MusicCacheIndexStore(context)
    private val lock = Any()

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun read(key: String): ByteArray? = synchronized(lock) {
        val target = fileForKey(key)
        if (!target.exists()) return@synchronized null

        val bytes = runCatching { target.readBytes() }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            runCatching { target.delete() }
            return@synchronized null
        }

        runCatching { target.setLastModified(System.currentTimeMillis()) }
        runCatching { metaFileForPayload(target).writeText(key, Charsets.UTF_8) }
        bytes
    }

    fun write(key: String, bytes: ByteArray) = synchronized(lock) {
        if (bytes.isEmpty()) return@synchronized

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val target = fileForKey(key)
        val temp = File(target.absolutePath + ".tmp")

        runCatching {
            temp.writeBytes(bytes)
            if (target.exists()) {
                target.delete()
            }
            temp.renameTo(target)
            target.setLastModified(System.currentTimeMillis())
            metaFileForPayload(target).writeText(key, Charsets.UTF_8)
        }.onFailure {
            runCatching { temp.delete() }
        }
    }

    fun writeFromFile(key: String, source: File) = synchronized(lock) {
        if (!source.exists() || source.length() <= 0L) return@synchronized

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val target = fileForKey(key)
        val temp = File(target.absolutePath + ".tmp")

        runCatching {
            source.copyTo(temp, overwrite = true)
            if (target.exists()) {
                target.delete()
            }
            temp.renameTo(target)
            target.setLastModified(System.currentTimeMillis())
            metaFileForPayload(target).writeText(key, Charsets.UTF_8)
        }.onFailure {
            runCatching { temp.delete() }
        }
    }

    fun resolveFile(key: String): File? = synchronized(lock) {
        val target = fileForKey(key)
        if (!target.exists() || target.length() <= 0L) {
            return@synchronized null
        }

        runCatching { target.setLastModified(System.currentTimeMillis()) }
        runCatching { metaFileForPayload(target).writeText(key, Charsets.UTF_8) }
        target
    }

    fun remove(key: String) = synchronized(lock) {
        if (key.isBlank()) return@synchronized

        val target = fileForKey(key)
        runCatching { target.delete() }
        runCatching { metaFileForPayload(target).delete() }
    }

    fun totalBytes(): Long = synchronized(lock) {
        return@synchronized payloadFiles().sumOf { it.length() } +
            progressiveMusicFiles().sumOf { it.length() }
    }

    fun clear() = synchronized(lock) {
        payloadFiles().forEach { file ->
            runCatching { file.delete() }
            runCatching { metaFileForPayload(file).delete() }
        }
        progressiveMusicFiles().forEach { file ->
            runCatching { file.delete() }
        }
        musicCacheIndexStore.clear()
    }

    fun collectStorageStats(): StorageStats = synchronized(lock) {
        val totals = mutableMapOf<StorageCategory, Long>().apply {
            StorageCategory.entries.forEach { category ->
                this[category] = 0L
            }
        }

        payloadFiles().forEach { file ->
            val bytes = file.length().coerceAtLeast(0L)
            if (bytes <= 0L) return@forEach

            val sourceKey = runCatching {
                metaFileForPayload(file)
                    .takeIf { it.exists() }
                    ?.readText(Charsets.UTF_8)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()

            val category = resolveCategory(sourceKey)
            totals[category] = (totals[category] ?: 0L) + bytes
        }

        progressiveMusicFiles().forEach { file ->
            val bytes = file.length().coerceAtLeast(0L)
            if (bytes <= 0L) return@forEach
            totals[StorageCategory.Music] = (totals[StorageCategory.Music] ?: 0L) + bytes
        }

        StorageStats(
            totalBytes = totals.values.sum(),
            categoryBytes = totals.toMap()
        )
    }

    fun clearByCategories(categories: Set<StorageCategory>): StorageClearResult = synchronized(lock) {
        if (categories.isEmpty()) return@synchronized StorageClearResult()

        var deletedFiles = 0
        var deletedBytes = 0L

        payloadFiles().forEach { file ->
            val sourceKey = runCatching {
                metaFileForPayload(file)
                    .takeIf { it.exists() }
                    ?.readText(Charsets.UTF_8)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()

            val category = resolveCategory(sourceKey)
            if (!categories.contains(category)) return@forEach

            val bytes = file.length().coerceAtLeast(0L)
            val deleted = runCatching { file.delete() }.getOrDefault(false)
            runCatching { metaFileForPayload(file).delete() }

            if (deleted) {
                deletedFiles += 1
                deletedBytes += bytes
            }
        }

        if (categories.contains(StorageCategory.Music)) {
            progressiveMusicFiles().forEach { file ->
                val bytes = file.length().coerceAtLeast(0L)
                val deleted = runCatching { file.delete() }.getOrDefault(false)
                if (deleted) {
                    deletedFiles += 1
                    deletedBytes += bytes
                }
            }
            musicCacheIndexStore.clear()
        }

        StorageClearResult(
            filesDeleted = deletedFiles,
            bytesDeleted = deletedBytes
        )
    }

    private fun fileForKey(key: String): File {
        return File(cacheDir, key.sha256Hex())
    }

    private fun payloadFiles(): List<File> {
        return cacheDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    !file.name.endsWith(META_SUFFIX) &&
                    !file.name.endsWith(TMP_SUFFIX)
            }
            ?: emptyList()
    }

    private fun progressiveMusicFiles(): List<File> {
        return musicProgressiveDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.length() > 0L &&
                    file.name.endsWith(".part")
            }
            ?: emptyList()
    }

    private fun metaFileForPayload(payloadFile: File): File {
        return File(cacheDir, payloadFile.name + META_SUFFIX)
    }

    companion object {
        private const val META_SUFFIX = ".meta"
        private const val TMP_SUFFIX = ".tmp"

        fun resolveCategory(cacheKey: String?): StorageCategory {
            val key = cacheKey?.lowercase().orEmpty()

            return when {
                key.contains("avatars") -> StorageCategory.Avatars
                key.contains("music/files") -> StorageCategory.Music
                key.contains("music/covers") || key.contains("covers") -> StorageCategory.Covers
                key.contains("posts/images") || key.contains("posts/videos") -> StorageCategory.Posts
                key.contains("comments/images") -> StorageCategory.Comments
                key.contains("downloads") || key.contains("files") || key.contains("messenger") -> {
                    StorageCategory.Messenger
                }
                else -> StorageCategory.Messenger
            }
        }
    }
}

private fun String.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(toByteArray(Charsets.UTF_8))
    return buildString(hash.size * 2) {
        hash.forEach { byte ->
            append(((byte.toInt() ushr 4) and 0x0f).toString(16))
            append((byte.toInt() and 0x0f).toString(16))
        }
    }
}
