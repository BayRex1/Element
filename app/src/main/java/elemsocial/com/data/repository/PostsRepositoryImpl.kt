package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.PostsRemoteDataSource
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.CommentsResult
import elemsocial.com.domain.model.DownloadChunkResult
import elemsocial.com.domain.model.FeedResult
import elemsocial.com.domain.model.OnlineUser
import elemsocial.com.domain.model.PollVoteResult
import elemsocial.com.domain.model.PostDetailsResult
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.domain.model.UploadFilePayload
import elemsocial.com.domain.repository.PostsRepository
import java.io.ByteArrayOutputStream

class PostsRepositoryImpl(
    socketClient: ElementSocketClient
) : PostsRepository {
    private val remote = PostsRemoteDataSource(socketClient)

    override suspend fun loadOnlineUsers(): List<OnlineUser> {
        val response = remote.loadOnlineUsers()
        val users = response["users"] as? List<*> ?: return emptyList()
        return users.mapNotNull { item ->
            val map = item.asMap() ?: return@mapNotNull null
            val id = map["id"].asInt() ?: return@mapNotNull null
            val name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: "User"
            val username = map["username"]?.toString()?.takeIf { it.isNotBlank() } ?: "unknown"
            OnlineUser(
                id = id,
                name = name,
                username = username,
                avatar = parseAsset(map["avatar"])
            )
        }
    }

    override suspend fun createPost(
        text: String,
        files: List<UploadFilePayload>,
        songs: List<Int>,
        fromChannelId: Int?,
        wallUsername: String?,
        poll: PostPoll?,
        clearMetadataImage: Boolean,
        censoringImage: Boolean
    ): ActionResult {
        val payloadFiles = files.map {
            mapOf(
                "name" to it.name,
                "type" to it.mimeType,
                "size" to it.size,
                "buffer" to it.bytes
            )
        }
        val response = remote.createPost(
            text = text,
            files = payloadFiles,
            songs = songs,
            from = fromChannelId?.let { channelId -> mapOf("type" to 1, "id" to channelId) },
            wallUsername = wallUsername,
            poll = poll?.let { composerPoll ->
                mapOf(
                    "question" to composerPoll.question,
                    "options" to composerPoll.options.map { it.text },
                    "is_anonymous" to composerPoll.isAnonymous,
                    "multiple_choice" to composerPoll.multipleChoice
                )
            },
            clearMetadataImage = clearMetadataImage,
            censoringImage = censoringImage
        )
        return actionResult(response)
    }

    override suspend fun editPost(postId: Int, text: String): ActionResult =
        actionResult(remote.editPost(postId, text))

    override suspend fun loadPosts(category: PostsCategory, startIndex: Int): FeedResult {
        val response = remote.loadPosts(category, startIndex)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("posts")) "success" else "error"
        return FeedResult(
            status = status,
            message = response["message"]?.toString(),
            posts = parseFeedPosts(response["posts"]),
            raw = response
        )
    }

    override suspend fun loadPost(postId: Int): PostDetailsResult {
        val response = remote.loadPost(postId)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("post")) "success" else "error"
        return PostDetailsResult(
            status = status,
            message = response["message"]?.toString(),
            post = parseFeedPost(response["post"])
        )
    }

    override suspend fun loadComments(postId: Int): CommentsResult {
        val response = remote.loadComments(postId)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("comments")) "success" else "error"
        return CommentsResult(
            status = status,
            message = response["message"]?.toString(),
            comments = parseComments(response["comments"])
        )
    }

    override suspend fun addComment(
        postId: Int,
        text: String,
        replyToCommentId: Int?,
        files: List<UploadFilePayload>
    ): ActionResult {
        val payloadFiles = files.map {
            mapOf(
                "name" to it.name,
                "type" to it.mimeType,
                "size" to it.size,
                "buffer" to it.bytes
            )
        }
        return actionResult(
            remote.addComment(
                postId = postId,
                text = text,
                replyToCommentId = replyToCommentId,
                files = payloadFiles
            )
        )
    }

    override suspend fun deleteComment(commentId: Int): ActionResult =
        actionResult(remote.deleteComment(commentId))

    override suspend fun likePost(postId: Int): Boolean =
        isActionSuccessful(remote.likePost(postId))

    override suspend fun dislikePost(postId: Int): Boolean =
        isActionSuccessful(remote.dislikePost(postId))

    override suspend fun setReaction(postId: Int, reaction: String): Boolean =
        isActionSuccessful(remote.setReaction(postId, reaction))

    override suspend fun unsetReaction(postId: Int, reaction: String): Boolean =
        isActionSuccessful(remote.unsetReaction(postId, reaction))

    override suspend fun votePostPoll(postId: Int, optionIds: List<Int>): PollVoteResult {
        val response = remote.votePostPoll(postId, optionIds)
        val status = response["status"]?.toString()?.lowercase()
        val normalized = when (status) {
            "success", "ok", "200", null, "" -> "success"
            else -> "error"
        }
        return PollVoteResult(
            status = normalized,
            message = response["message"]?.toString(),
            poll = parsePoll(response["poll"])
        )
    }

    override suspend fun deletePost(postId: Int): ActionResult =
        actionResult(remote.deletePost(postId))

    override suspend fun restorePost(postId: Int): ActionResult =
        actionResult(remote.restorePost(postId))

    override suspend fun deletePostForever(postId: Int): ActionResult =
        actionResult(remote.deletePostForever(postId))

    override suspend fun addPostToArchive(postId: Int): ActionResult =
        actionResult(remote.addPostToArchive(postId))

    override suspend fun removePostFromArchive(postId: Int): ActionResult =
        actionResult(remote.removePostFromArchive(postId))

    override suspend fun blockProfile(username: String): ActionResult =
        actionResult(remote.blockProfile(username))

    override suspend fun unblockProfile(username: String): ActionResult =
        actionResult(remote.unblockProfile(username))

    // === Images ===

    override suspend fun downloadImage(asset: PostImageAsset, preferLossless: Boolean): ByteArray? {
        val fileId = asset.fileId
        if (fileId != null && fileId > 0) {
            val variants = if (preferLossless) listOf("original", "webp", "avif")
            else listOf("webp", "avif", "original")
            for (variant in variants) {
                val bytes = downloadStorageBytes(fileId, variant)
                if (bytes != null && bytes.isNotEmpty()) return bytes
            }
        }

        if (asset.path.isNotBlank() && asset.file.isNotBlank()) {
            val response = runCatching { remote.downloadImage(asset, preferLossless) }.getOrNull()
                ?: return null
            val statusCode = response["status"].asInt(-1)
            if (statusCode != 200) return null
            val preferred = if (preferLossless) {
                response["file"].asMap()
            } else {
                response["simple"].asMap() ?: response["file"].asMap()
            }
            return preferred?.get("buffer") as? ByteArray
                ?: response["buffer"] as? ByteArray
        }

        return null
    }

    private suspend fun downloadStorageBytes(fileId: Int, variant: String): ByteArray? {
        val metadataResponse = runCatching {
            remote.getStorageFileData(fileId, variant)
        }.getOrNull() ?: return null

        val fileData = metadataResponse["file_data"].asRichMap() ?: return null
        val selected = if (variant == "original") {
            fileData
        } else {
            fileData["variants"].asRichMap()?.get(variant).asRichMap() ?: return null
        }

        if (selected["variant_status"]?.toString()?.equals("processing", ignoreCase = true) == true) {
            return null
        }

        val expectedSize = selected["size"].asLong(0L) ?: 0L
        if (expectedSize <= 0L) return null

        val out = ByteArrayOutputStream()
        var offset = 0L
        var iterations = 0

        while (offset < expectedSize && iterations < 4096) {
            iterations++
            val response = runCatching {
                remote.downloadStorageChunk(fileId, offset, variant)
            }.getOrNull() ?: return null

            val chunk = extractBuffer(response) ?: return null
            if (chunk.isEmpty()) return null

            val responseOffset = response["offset"].asLong(offset) ?: offset
            if (responseOffset != offset) return null

            out.write(chunk)
            offset += chunk.size
        }

        if (offset != expectedSize) return null
        return out.toByteArray().takeIf { it.isNotEmpty() }
    }

    override suspend fun downloadFileChunk(
        path: String,
        file: String,
        offset: Long
    ): DownloadChunkResult {
        val response = remote.downloadFileChunk(path, file, offset)
        return DownloadChunkResult(
            statusCode = response["status"].asInt(-1) ?: -1,
            buffer = extractBuffer(response) ?: ByteArray(0),
            totalSize = response["total_size"].asLong(0L) ?: 0L,
            offset = response["offset"].asLong(offset) ?: offset,
            isLastChunk = response["is_last_chunk"].asBoolean()
        )
    }

    override suspend fun downloadStorageChunk(
        fileId: Int,
        offset: Long,
        variant: String
    ): Map<String, Any?> = remote.downloadStorageChunk(fileId, offset, variant)

    private fun extractBuffer(response: Map<String, Any?>?): ByteArray? {
        if (response == null) return null
        val raw = response["buffer"]
        return when (raw) {
            is ByteArray -> raw
            is List<*> -> {
                val bytes = ByteArray(raw.size)
                for (i in raw.indices) {
                    val v = raw[i]
                    if (v !is Number) return null
                    bytes[i] = v.toByte()
                }
                bytes
            }
            else -> null
        }
    }

    private fun actionResult(response: Map<String, Any?>): ActionResult {
        val status = response["status"]?.toString()?.lowercase()
        val normalized = when (status) {
            "success", "ok", "200", null, "" -> "success"
            else -> "error"
        }
        return ActionResult(status = normalized, message = response["message"]?.toString())
    }

    private fun isActionSuccessful(response: Map<String, Any?>): Boolean {
        val status = response["status"]?.toString()?.lowercase()
        return when (status) {
            "error" -> false
            "success", "ok", "200" -> true
            null, "" -> true
            else -> status != "error"
        }
    }
}
