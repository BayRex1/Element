package elemsocial.com.feature.home.presentation

import elemsocial.com.core.cache.ImageDiskCache
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.CommentsResult
import elemsocial.com.domain.model.FeedResult
import elemsocial.com.domain.model.OnlineUser
import elemsocial.com.domain.model.PollVoteResult
import elemsocial.com.domain.model.PostDetailsResult
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

    suspend fun editPost(
        postId: Int,
        text: String
    ): ActionResult {
        return postsRepository.editPost(
            postId = postId,
            text = text
        )
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

    suspend fun likePost(postId: Int): Boolean {
        return postsRepository.likePost(postId)
    }

    suspend fun dislikePost(postId: Int): Boolean {
        return postsRepository.dislikePost(postId)
    }

    suspend fun votePostPoll(postId: Int, optionIds: List<Int>): PollVoteResult {
        return postsRepository.votePostPoll(postId, optionIds)
    }

    suspend fun deletePost(postId: Int): ActionResult {
        return postsRepository.deletePost(postId)
    }

    suspend fun restorePost(postId: Int): ActionResult {
        return postsRepository.restorePost(postId)
    }

    suspend fun deletePostForever(postId: Int): ActionResult {
        return postsRepository.deletePostForever(postId)
    }

    suspend fun addPostToArchive(postId: Int): ActionResult {
        return postsRepository.addPostToArchive(postId)
    }

    suspend fun removePostFromArchive(postId: Int): ActionResult {
        return postsRepository.removePostFromArchive(postId)
    }

    suspend fun blockProfile(username: String): ActionResult {
        return postsRepository.blockProfile(username)
    }

    suspend fun unblockProfile(username: String): ActionResult {
        return postsRepository.unblockProfile(username)
    }

    suspend fun loadImageBytes(asset: PostImageAsset): ByteArray? {
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
        val cache = imageDiskCache ?: return null
        return withContext(Dispatchers.IO) {
            cache.resolveFile(cacheKey)
        }
    }

    suspend fun loadVideoFile(
        video: PostVideo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false }
    ): File? {
        resolveCachedFile(video.cacheKey)?.let { cached ->
            onProgress(cached.length(), cached.length())
            return cached
        }

        val out = ByteArrayOutputStream()
        var downloadedBytes = 0L
        var totalBytes = video.fileSize ?: 0L

        val firstChunk = postsRepository.downloadFileChunk(
            path = video.path,
            file = video.file,
            offset = 0L
        )
        if (firstChunk.statusCode != 200) {
            return null
        }
        if (firstChunk.buffer.isEmpty() && !firstChunk.isLastChunk) {
            return null
        }

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
                    if (isCancelled()) {
                        return null
                    }

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

                    results
                        .sortedBy { it.first }
                        .forEach { (_, chunk) ->
                            if (chunk.statusCode != 200) {
                                return null
                            }
                            if (chunk.buffer.isEmpty() && !chunk.isLastChunk) {
                                return null
                            }
                            out.write(chunk.buffer)
                            downloadedBytes += chunk.buffer.size
                            onProgress(downloadedBytes, totalBytes)
                        }
                }
            } else {
                var offset = firstChunk.buffer.size.toLong()
                var isLastChunk = false

                while (!isLastChunk) {
                    if (isCancelled()) {
                        return null
                    }

                    val chunk = postsRepository.downloadFileChunk(
                        path = video.path,
                        file = video.file,
                        offset = offset
                    )
                    if (chunk.statusCode != 200) {
                        return null
                    }
                    if (chunk.buffer.isEmpty() && !chunk.isLastChunk) {
                        return null
                    }

                    out.write(chunk.buffer)
                    offset += chunk.buffer.size
                    downloadedBytes += chunk.buffer.size
                    totalBytes = maxOf(totalBytes, chunk.totalSize)
                    onProgress(downloadedBytes, totalBytes)
                    isLastChunk = chunk.isLastChunk
                }
            }
        }

        if (isCancelled()) {
            return null
        }

        val payload = out.toByteArray()
        if (payload.isEmpty()) {
            return null
        }

        imageDiskCache?.let { cache ->
            withContext(Dispatchers.IO) {
                cache.write(video.cacheKey, payload)
            }
        }

        return resolveCachedFile(video.cacheKey)
    }

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
