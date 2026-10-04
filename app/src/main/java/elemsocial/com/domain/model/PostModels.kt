package elemsocial.com.domain.model

enum class PostsCategory(
    val apiValue: String,
    val title: String
) {
    Last("last", "Последние"),
    Recommended("rec", "Рекомендации"),
    Subscriptions("subscribe", "Подписки")
}

data class ActionResult(
    val status: String,
    val message: String? = null
) {
    val isSuccess: Boolean
        get() = status != "error"
}

data class FeedResult(
    val status: String,
    val message: String? = null,
    val posts: List<FeedPost> = emptyList(),
    val raw: Map<String, Any?> = emptyMap()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class PostDetailsResult(
    val status: String,
    val message: String? = null,
    val post: FeedPost? = null
) {
    val isSuccess: Boolean get() = status == "success" && post != null
}

data class CommentsResult(
    val status: String,
    val message: String? = null,
    val comments: List<PostComment> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class PostReactions(
    val results: Map<String, Int> = emptyMap(),
    val userReactions: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = results.isEmpty() && userReactions.isEmpty()
}

data class FeedPost(
    val id: Int,
    val author: PostAuthor? = null,
    val text: String? = null,
    val poll: PostPoll? = null,
    val content: PostContent = PostContent(),
    val createDate: String? = null,
    val editedAt: String? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val comments: Int = 0,
    val liked: Boolean = false,
    val disliked: Boolean = false,
    val myPost: Boolean = false,
    val deleted: Boolean = false,
    val archived: Boolean = false,
    val reactions: PostReactions? = null
)

data class PostPoll(
    val id: Int,
    val question: String = "",
    val isAnonymous: Boolean = false,
    val multipleChoice: Boolean = false,
    val expiresAt: String? = null,
    val totalVotes: Int = 0,
    val userVote: List<Int> = emptyList(),
    val options: List<PostPollOption> = emptyList()
)

data class PostPollOption(
    val id: Int,
    val text: String,
    val votesCount: Int = 0
)

data class PollVoteResult(
    val status: String,
    val message: String? = null,
    val poll: PostPoll? = null
) {
    val isSuccess: Boolean get() = status == "success" && poll != null
}

data class PostAuthor(
    val id: Int? = null,
    val type: Int? = null,
    val username: String? = null,
    val name: String? = null,
    val avatar: PostImageAsset? = null,
    val icons: List<String> = emptyList(),
    val blocked: Boolean = false,
    val deleted: Boolean = false
)

data class PostContent(
    val images: List<PostImage> = emptyList(),
    val videos: List<PostVideo> = emptyList(),
    val files: List<PostFile> = emptyList(),
    val filesCount: Int = 0,
    val videosCount: Int = 0,
    val songs: List<PostSong> = emptyList()
) {
    val songsCount: Int
        get() = songs.size
}

data class PostFile(
    val id: Int = 0,
    val fileId: String? = null,
    val name: String = "",
    val size: Long = 0L,
    val mimeType: String? = null,
    val path: String = "posts/files",
    val file: String = ""
) {
    val cacheKey: String
        get() = if (fileId != null) "file:$fileId" else "$path/$file"
}

data class PostSong(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String? = null,
    val cover: PostImageAsset? = null,
    val durationSeconds: Double? = null
)

data class PostImage(
    val asset: PostImageAsset? = null,
    val fileName: String? = null,
    val fileSize: Long? = null
)

data class PostVideo(
    val path: String = "posts/videos",
    val file: String,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val preview: PostImageAsset? = null,
    val info: PostVideoInfo? = null
) {
    val cacheKey: String
        get() = "$path/$file"
}

data class PostVideoInfo(
    val width: Int? = null,
    val height: Int? = null
)

data class PostImageAsset(
    val path: String,
    val file: String,
    val simple: String? = null,
    val aura: String? = null,
    val preview: String? = null
) {
    val cacheKey: String
        get() = "$path/$file/${simple.orEmpty()}"
}

data class OnlineUser(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: PostImageAsset? = null
)

data class UploadFilePayload(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    val size: Int get() = bytes.size
}

data class DownloadChunkResult(
    val statusCode: Int,
    val buffer: ByteArray = ByteArray(0),
    val totalSize: Long = 0L,
    val isLastChunk: Boolean = true
)

data class PostComment(
    val id: Int,
    val postId: Int,
    val author: PostAuthor? = null,
    val text: String = "",
    val content: PostCommentContent = PostCommentContent(),
    val date: String? = null,
    val deleted: Boolean = false
)

data class PostCommentContent(
    val reply: PostCommentReply? = null,
    val images: List<PostImage> = emptyList(),
    val filesCount: Int = 0
)

data class PostCommentReply(
    val commentId: Int? = null,
    val author: PostAuthor? = null,
    val text: String = ""
)
