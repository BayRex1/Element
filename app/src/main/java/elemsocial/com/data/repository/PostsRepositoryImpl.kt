package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.PostsRemoteDataSource
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.CommentsResult
import elemsocial.com.domain.model.DownloadChunkResult
import elemsocial.com.domain.model.FeedPost
import elemsocial.com.domain.model.FeedResult
import elemsocial.com.domain.model.OnlineUser
import elemsocial.com.domain.model.PollVoteResult
import elemsocial.com.domain.model.PostComment
import elemsocial.com.domain.model.PostDetailsResult
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.domain.model.UploadFilePayload
import elemsocial.com.domain.repository.PostsRepository

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
            from = fromChannelId?.let { channelId ->
                mapOf(
                    "type" to 1,
                    "id" to channelId
                )
            },
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

    override suspend fun editPost(postId: Int, text: String): ActionResult {
        val response = remote.editPost(
            postId = postId,
            text = text
        )
        return actionResult(response)
    }

    override suspend fun loadPosts(category: PostsCategory, startIndex: Int): FeedResult {
        val response = remote.loadPosts(category, startIndex)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("posts")) "success" else "error"
        val message = response["message"]?.toString()
        val posts = parseFeedPosts(response["posts"])

        return FeedResult(
            status = status,
            message = message,
            posts = posts,
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
        val response = remote.addComment(
            postId = postId,
            text = text,
            replyToCommentId = replyToCommentId,
            files = payloadFiles
        )
        return actionResult(response)
    }

    override suspend fun deleteComment(commentId: Int): ActionResult {
        val response = remote.deleteComment(commentId)
        return actionResult(response)
    }

    override suspend fun likePost(postId: Int): Boolean {
        val response = remote.likePost(postId)
        return isActionSuccessful(response)
    }

    override suspend fun dislikePost(postId: Int): Boolean {
        val response = remote.dislikePost(postId)
        return isActionSuccessful(response)
    }

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

    override suspend fun deletePost(postId: Int): ActionResult {
        val response = remote.deletePost(postId)
        return actionResult(response)
    }

    override suspend fun restorePost(postId: Int): ActionResult {
        val response = remote.restorePost(postId)
        return actionResult(response)
    }

    override suspend fun deletePostForever(postId: Int): ActionResult {
        val response = remote.deletePostForever(postId)
        return actionResult(response)
    }

    override suspend fun addPostToArchive(postId: Int): ActionResult {
        val response = remote.addPostToArchive(postId)
        return actionResult(response)
    }

    override suspend fun removePostFromArchive(postId: Int): ActionResult {
        val response = remote.removePostFromArchive(postId)
        return actionResult(response)
    }

    override suspend fun blockProfile(username: String): ActionResult {
        val response = remote.blockProfile(username)
        return actionResult(response)
    }

    override suspend fun unblockProfile(username: String): ActionResult {
        val response = remote.unblockProfile(username)
        return actionResult(response)
    }

    override suspend fun downloadImage(asset: PostImageAsset, preferLossless: Boolean): ByteArray? {
        val response = remote.downloadImage(asset, preferLossless)
        val statusCode = response["status"].asInt(-1)
        if (statusCode != 200) {
            return null
        }

        val preferred = if (preferLossless) {
            response["file"].asMap()
        } else {
            response["simple"].asMap() ?: response["file"].asMap()
        }

        return preferred?.get("buffer") as? ByteArray
    }

    override suspend fun downloadFileChunk(path: String, file: String, offset: Long): DownloadChunkResult {
        val response = remote.downloadFileChunk(
            path = path,
            file = file,
            offset = offset
        )

        return DownloadChunkResult(
            statusCode = response["status"].asInt(-1) ?: -1,
            buffer = response["buffer"] as? ByteArray ?: ByteArray(0),
            totalSize = response["total_size"].asLong(0L) ?: 0L,
            isLastChunk = response["is_last_chunk"].asBoolean()
        )
    }

    private fun actionResult(response: Map<String, Any?>): ActionResult {
        val status = response["status"]?.toString()?.lowercase()
        val normalized = when (status) {
            "success", "ok", "200", null, "" -> "success"
            else -> "error"
        }
        return ActionResult(
            status = normalized,
            message = response["message"]?.toString()
        )
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
