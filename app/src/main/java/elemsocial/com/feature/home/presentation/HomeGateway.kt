package elemsocial.com.feature.home.presentation

import elemsocial.com.core.cache.ImageDiskCache
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.CommentsResult
import elemsocial.com.domain.model.FeedResult
import elemsocial.com.domain.model.OnlineUser
import elemsocial.com.domain.model.PollVoteResult
import elemsocial.com.domain.model.PostDetailsResult
import elemsocial.com.domain.model.PostFile
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostVideo
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.domain.model.UploadFilePayload
import elemsocial.com.domain.repository.PostsRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.LinkedHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeGateway(
    private val postsRepository: PostsRepository,
    private val imageDiskCache: ImageDiskCache? = null
) {
    private companion object {
        const val VideoChunkSizeBytes = 512 * 1024L
        const val ParallelVideoChunkRequests = 4
    }

    private val imageCache = object : LinkedHashMap<String, ByteArray>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > 120
        }
    }

    suspend fun loadPosts(category: PostsCategory, startIndex: Int): FeedResult {
        return postsRepository.loadPosts(category, startIndex)
    }

    suspend fun createPost(
        text: String,
        files: List<UploadFilePayload> = emptyList(),
        songs: List<Int> = emptyList(),
        fromChannelId: Int? = null,
        wallUsername: String? = null,
        poll: PostPoll? = null,
        clearMetadataImage: Boolean = false,
        censoringImage: Boolean = false
    ): ActionResult {
        return postsRepository.createPost(
            text = text,
            files = files,
            songs = songs,
            fromChannelId = fromChannelId,
            wallUsername = wallUsername,
            poll = poll,
            clearMetadataImage = clearMetadataImage,
            censoringImage = censoringImage
        )
    }

    suspend fun editPost(postId: Int, text: String): ActionResult {
        return postsRepository.editPost(postId = postId, text = text)
    }

    suspend fun loadOnlineUsers(): List<OnlineUser> {
        return postsRepository.loadOnlineUsers()
    }

    suspend fun loadPost(postId: Int): PostDetailsResult {
        return postsRepository.loadPost(postId)
    }

    suspend fun loadComments(postId: Int): CommentsResult {
        return postsRepository.loadComments(postId)
    }

    suspend fun addComment(
        postId: Int,
        text: String,
        replyToCommentId: Int? = null,
        files: List<UploadFilePayload> = emptyList()
    ): ActionResult {
        return postsRepository.addComment(
            postId = postId,
            text = text,
            replyToCommentId = replyToCommentId,
            files = files
        )
    }

    suspend fun deleteComment(commentId: Int): ActionResult {
        return postsRepository.deleteComment(commentId)
    }

    suspend fun likePost(postId: Int): Boolean = postsRepository.likePost(postId)

    suspend fun dislikePost(postId: Int): Boolean = postsRepository.dislikePost(postId)

    suspend fun setReaction(postId: Int, reaction: String): Boolean =
        postsRepository.setReaction(postId, reaction)

    suspend fun unsetReaction(postId: Int, reaction: String): Boolean =
        postsRepository.unsetReaction(postId, reaction)

    suspend fun votePostPoll(postId: Int, optionIds: List<Int>): PollVoteResult =
        postsRepository.votePostPoll(postId, optionIds)

    suspend fun deletePost(postId: Int): ActionResult = postsRepository.deletePost(postId)

    suspend fun restorePost(postId: Int): ActionResult = postsRepository.restorePost(postId)

    suspend fun deletePostForever(postId: Int): ActionResult = postsRepository.deletePostForever(postId)

    suspend fun addPostToArchive(postId: Int): ActionResult = postsRepository.addPostToArchive(postId)

    suspend fun removePostFromArchive(postId: Int): ActionResult =
        postsRepository.removePostFromArchive(postId)

    suspend fun blockProfile(username: String): ActionResult = postsRepository.blockProfile(username)

    suspend fun unblockProfile(username: String): ActionResult = postsRepository.unblockProfile(username)

    // === Images (avatars, post images, covers) ===

    suspend fun loadImageBytes(asset: PostImageAsset): ByteArray? {
        if (asset.isEmpty) return null

        val key = asset.cacheKey
        synchronized(imageCache) {
            imageCache[key]?.let { return it }
        }

        val persisted: ByteArray? = imageDiskCache?.let { cache ->
            withContext(Dispatchers.IO) { cache.read(key) }
        }
        if (persisted != null && persisted.isNotEmpty()) {
            synchronized(imageCache) {
                imageCache[key] = persisted
            }
            return persisted
        }

        val downloaded = postsRepository.downloadImage(asset, preferLossless = false)
            ?: postsRepository.downloadImage(asset, preferLossless = true)
            ?: return null

        synchronized(imageCache) {
            imageCache[key] = downloaded
        }
        imageDiskCache?.let { cache ->
            withContext(Dispatchers.IO) { cache.write(key, downloaded) }
        }
        return downloaded
    }

    suspend fun evictImage(asset: PostImageAsset?) {
        val key = asset?.cacheKey?.takeIf { it.isNotBlank() } ?: return
        synchronized(imageCache) {
            imageCache.remove(key)
        }
        imageDiskCache?.let { cache ->
            withContext(Dispatchers.IO) { cache.remove(key) }
        }
    }

    suspend fun resolveCachedFile(cacheKey: String): File? {
        if (cacheKey.isBlank()) return null
        val cache = imageDiskCache ?: return null
        return withContext(Dispatchers.IO) {
            cache.resolveFile(cacheKey)
        }
    }

    // === Videos ===

    suspend fun loadVideoFile(
        video: PostVideo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false }
    ): File? {
        if (video.isEmpty) return null

        resolveCachedFile(video.cacheKey)?.let { cached ->
            onProgress(cached.length(), cached.length())
            return cached
        }

        val id = video.fileId
        if (id != null && id > 0) {
            val payload = downloadStorageFileBytes(id, video.fileSize ?: 0L, onProgress, isCancelled)
            if (payload == null || payload.isEmpty()) return null
            imageDiskCache?.let { cache ->
                withContext(Dispatchers.IO) { cache.write(video.cacheKey, payload) }
            }
            return resolveCachedFile(video.cacheKey)
        }

        if (video.path.isBlank() || video.file.isBlank()) return null
        return downloadVideoByPath(video, onProgress, isCancelled)
    }

    private suspend fun downloadVideoByPath(
        video: PostVideo,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean
    ): File? {
        val out = ByteArrayOutputStream()
        var downloadedBytes = 0L
        var totalBytes = video.fileSize ?: 0L

        val firstChunk = postsRepository.downloadFileChunk(
            path = video.path,
            file = video.file,
            offset = 0L
        )
        if (firstChunk.statusCode != 200 && firstChunk.statusCode != -1) return null
        if (firstChunk.buffer.isEmpty() && !firstChunk.isLastChunk) return null

        out.write(firstChunk.buffer)
        downloadedBytes += firstChunk.buffer.size
        totalBytes = maxOf(totalBytes, firstChunk.totalSize)
        onProgress(downloadedBytes, totalBytes)

        if (!firstChunk.isLastChunk) {
            if (totalBytes > 0L) {
                val remainingOffsets = generateSequence(VideoChunkSizeBytes) { previous ->
                    (previous + VideoChunkSizeBytes).takeIf { it < totalBytes }
                }.toList()

                for (batch in remainingOffsets.chunked(ParallelVideoChunkRequests)) {
                    if (isCancelled()) return null
                    val results = coroutineScope {
                        batch.map { offset ->
                            async {
                                offset to postsRepository.downloadFileChunk(
                                    path = video.path,
                                    file = video.file,
                                    offset = offset
                                )
                            }
                        }.awaitAll()
                    }
                    results.sortedBy { it.first }.forEach { (_, chunk) ->
                        if (chunk.statusCode != 200 && chunk.statusCode != -1) return null
                        if (chunk.buffer.isEmpty() && !chunk.isLastChunk) return null
                        out.write(chunk.buffer)
                        downloadedBytes += chunk.buffer.size
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            } else {
                var offset = firstChunk.buffer.size.toLong()
                var isLast = false
                var iterations = 0
                while (!isLast && iterations < 4096) {
                    iterations++
                    if (isCancelled()) return null
                    val chunk = postsRepository.downloadFileChunk(
                        path = video.path,
                        file = video.file,
                        offset = offset
                    )
                    if (chunk.statusCode != 200 && chunk.statusCode != -1) return null
                    if (chunk.buffer.isEmpty() && !chunk.isLastChunk) return null
                    out.write(chunk.buffer)
                    offset += chunk.buffer.size
                    downloadedBytes += chunk.buffer.size
                    totalBytes = maxOf(totalBytes, chunk.totalSize)
                    onProgress(downloadedBytes, totalBytes)
                    isLast = chunk.isLastChunk
                }
            }
        }

        if (isCancelled()) return null
        val payload = out.toByteArray()
        if (payload.isEmpty()) return null

        imageDiskCache?.let { cache ->
            withContext(Dispatchers.IO) { cache.write(video.cacheKey, payload) }
        }
        return resolveCachedFile(video.cacheKey)
    }

    // === Files (documents in posts) ===

    suspend fun loadFile(
        file: PostFile,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false }
    ): File? {
        if (file.isEmpty) return null

        resolveCachedFile(file.cacheKey)?.let { cached ->
            onProgress(cached.length(), cached.length())
            return cached
        }

        val payload = run {
            val id = file.fileId
            if (id != null && id > 0) {
                downloadStorageFileBytes(id, file.size, onProgress, isCancelled)
            } else if (file.path.isNotBlank() && file.file.isNotBlank()) {
                downloadFileByPath(file, onProgress, isCancelled)
            } else {
                null
            }
        } ?: return null

        if (payload.isEmpty()) return null

        imageDiskCache?.let { cache ->
            withContext(Dispatchers.IO) { cache.write(file.cacheKey, payload) }
        }
        return resolveCachedFile(file.cacheKey)
    }

    private suspend fun downloadFileByPath(
        file: PostFile,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean
    ): ByteArray? {
        val out = ByteArrayOutputStream()
        var downloaded = 0L
        var total = file.size
        var offset = 0L
        var isLast = false
        var iterations = 0

        while (!isLast && iterations < 4096) {
            iterations++
            if (isCancelled()) return null

            val chunk = postsRepository.downloadFileChunk(
                path = file.path,
                file = file.file,
                offset = offset
            )
            if (chunk.statusCode != 200 && chunk.statusCode != -1) return null
            if (chunk.buffer.isEmpty() && !chunk.isLastChunk) return null

            out.write(chunk.buffer)
            downloaded += chunk.buffer.size
            offset += chunk.buffer.size
            total = maxOf(total, chunk.totalSize)
            onProgress(downloaded, total)
            isLast = chunk.isLastChunk
        }
        return out.toByteArray().takeIf { it.isNotEmpty() }
    }

    // === Storage (Neo: file_id Int) ===

    private suspend fun downloadStorageFileBytes(
        fileId: Int,
        knownTotal: Long,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean
    ): ByteArray? {
        val out = ByteArrayOutputStream()
        var downloaded = 0L
        var total = knownTotal
        var offset = 0L
        var isLast = false
        var iterations = 0

        while (!isLast && iterations < 4096) {
            iterations++
            if (isCancelled()) return null

            val response = postsRepository.downloadStorageChunk(fileId, offset, "original")
            val chunk = extractGatewayBuffer(response)
            if (chunk == null) return null
            if (chunk.isEmpty()) break

            out.write(chunk)
            downloaded += chunk.size
            offset += chunk.size
            val rawTotal = response["total_size"]
            val responseTotal: Long = when (rawTotal) {
                is Number -> rawTotal.toLong()
                is String -> rawTotal.toLongOrNull() ?: 0L
                else -> 0L
            }
            total = maxOf(total, responseTotal)
            onProgress(downloaded, total)

            val rawLast = response["is_last_chunk"]
            if (rawLast is Boolean && rawLast) isLast = true
            if (chunk.size < 1024 && total <= 0L) isLast = true
        }

        return out.toByteArray().takeIf { it.isNotEmpty() }
    }

    private fun extractGatewayBuffer(response: Map<String, Any?>?): ByteArray? {
        if (response == null) return null

        val direct = response["buffer"]
        if (direct is ByteArray) return direct
        if (direct is List<*>) {
            val bytes = ByteArray(direct.size)
            for (i in direct.indices) {
                val v = direct[i]
                if (v !is Number) return null
                bytes[i] = v.toByte()
            }
            return bytes
        }

        val nested = (response["file"] as? Map<*, *>)?.get("buffer")
            ?: (response["data"] as? Map<*, *>)?.get("buffer")
        if (nested is ByteArray) return nested
        if (nested is List<*>) {
            val bytes = ByteArray(nested.size)
            for (i in nested.indices) {
                val v = nested[i]
                if (v !is Number) return null
                bytes[i] = v.toByte()
            }
            return bytes
        }

        return null
    }

    // === Storage stats ===

    suspend fun loadStorageStats(): ImageDiskCache.StorageStats? {
        val cache = imageDiskCache ?: return null
        return withContext(Dispatchers.IO) {
            cache.collectStorageStats()
        }
    }

    suspend fun clearStorageCategories(
        categories: Set<ImageDiskCache.StorageCategory>
    ): ImageDiskCache.StorageClearResult {
        if (categories.isEmpty()) return ImageDiskCache.StorageClearResult()

        synchronized(imageCache) {
            val iterator = imageCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val category = ImageDiskCache.resolveCategory(entry.key)
                if (categories.contains(category)) {
                    iterator.remove()
                }
            }
        }

        val cache = imageDiskCache ?: return ImageDiskCache.StorageClearResult()
        return withContext(Dispatchers.IO) {
            cache.clearByCategories(categories)
        }
    }
}
