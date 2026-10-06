package elemsocial.com.data.remote

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostsCategory

class PostsRemoteDataSource(
    private val socketClient: ElementSocketClient
) {
    suspend fun createPost(
        text: String,
        files: List<Map<String, Any?>> = emptyList(),
        songs: List<Int> = emptyList(),
        from: Map<String, Any?>? = null,
        wallUsername: String? = null,
        poll: Map<String, Any?>? = null,
        clearMetadataImage: Boolean = false,
        censoringImage: Boolean = false
    ): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>(
            "text" to text,
            "files" to files,
            "songs" to songs,
            "from" to from,
            "settings" to mapOf(
                "clear_metadata_img" to clearMetadataImage,
                "censoring_img" to censoringImage
            )
        )
        if (poll != null) payload["poll"] = poll
        if (!wallUsername.isNullOrBlank()) {
            payload["type"] = "wall"
            payload["wall"] = mapOf("username" to wallUsername)
        }
        return socketClient.sendRequest(
            mapOf("type" to "social", "action" to "posts/add", "payload" to payload)
        )
    }

    suspend fun editPost(postId: Int, text: String): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/edit",
                "payload" to mapOf("post_id" to postId, "text" to text)
            )
        )

    suspend fun loadOnlineUsers(): Map<String, Any?> =
        socketClient.sendRequest(mapOf("type" to "social", "action" to "get_online_users"))

    suspend fun loadPosts(category: PostsCategory, startIndex: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "load_posts",
                "payload" to mapOf(
                    "posts_type" to category.apiValue,
                    "start_index" to startIndex
                )
            )
        )

    suspend fun loadPost(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf("type" to "social", "action" to "load_post", "pid" to postId)
        )

    suspend fun loadComments(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "comments/load",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun addComment(
        postId: Int,
        text: String,
        replyToCommentId: Int? = null,
        files: List<Map<String, Any?>> = emptyList()
    ): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>(
            "post_id" to postId,
            "text" to text,
            "files" to files
        )
        if (replyToCommentId != null) payload["reply_to"] = replyToCommentId
        return socketClient.sendRequest(
            mapOf("type" to "social", "action" to "comments/add", "payload" to payload)
        )
    }

    suspend fun deleteComment(commentId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "comments/delete",
                "payload" to mapOf("comment_id" to commentId)
            )
        )

    suspend fun likePost(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/like",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun dislikePost(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/dislike",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun setReaction(postId: Int, reaction: String): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "post/set_reaction",
                "payload" to mapOf("post_id" to postId, "reaction" to reaction)
            )
        )

    suspend fun unsetReaction(postId: Int, reaction: String): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "post/unset_reaction",
                "payload" to mapOf("post_id" to postId, "reaction" to reaction)
            )
        )

    suspend fun votePostPoll(postId: Int, optionIds: List<Int>): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/vote",
                "payload" to mapOf("post_id" to postId, "option_ids" to optionIds)
            )
        )

    suspend fun deletePost(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/delete",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun restorePost(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/restore",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun deletePostForever(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/delete_forever",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun addPostToArchive(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/add_to_archive",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun removePostFromArchive(postId: Int): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "posts/remove_from_archive",
                "payload" to mapOf("post_id" to postId)
            )
        )

    suspend fun blockProfile(username: String): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf("type" to "social", "action" to "block_profile", "username" to username)
        )

    suspend fun unblockProfile(username: String): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf("type" to "social", "action" to "unblock_profile", "username" to username)
        )

    // === Legacy images (path/file) ===

    suspend fun downloadImage(
        asset: PostImageAsset,
        preferLossless: Boolean = false
    ): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "download",
                "action" to "image",
                "image" to mapOf(
                    "path" to asset.path,
                    "file" to asset.file,
                    "simple" to asset.simple
                ),
                "lossless" to preferLossless
            ),
            timeoutMs = 30_000
        )

    // === Legacy files (path/file, chunks) ===

    suspend fun downloadFileChunk(
        path: String,
        file: String,
        offset: Long
    ): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "download",
                "action" to "file",
                "payload" to mapOf("path" to path, "file" to file, "offset" to offset)
            ),
            timeoutMs = 30_000
        )

    // === Neo storage (file_id) ===

    suspend fun downloadStorageChunk(
        fileId: Int,
        offset: Long,
        variant: String = "webp"
    ): Map<String, Any?> =
        socketClient.sendRequest(
            mapOf(
                "type" to "storage",
                "action" to "download",
                "payload" to mapOf(
                    "file_id" to fileId,
                    "offset" to offset,
                    "variant" to variant
                )
            ),
            timeoutMs = 30_000
        )
}
