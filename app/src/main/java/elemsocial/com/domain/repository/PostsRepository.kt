package elemsocial.com.domain.repository

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.CommentsResult
import elemsocial.com.domain.model.DownloadChunkResult
import elemsocial.com.domain.model.FeedResult
import elemsocial.com.domain.model.OnlineUser
import elemsocial.com.domain.model.PostDetailsResult
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PollVoteResult
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.domain.model.UploadFilePayload

interface PostsRepository {
    suspend fun loadOnlineUsers(): List<OnlineUser>
    suspend fun createPost(
        text: String,
        files: List<UploadFilePayload> = emptyList(),
        songs: List<Int> = emptyList(),
        fromChannelId: Int? = null,
        wallUsername: String? = null,
        poll: PostPoll? = null,
        clearMetadataImage: Boolean = false,
        censoringImage: Boolean = false
    ): ActionResult
    suspend fun editPost(postId: Int, text: String): ActionResult
    suspend fun loadPosts(category: PostsCategory, startIndex: Int): FeedResult
    suspend fun loadPost(postId: Int): PostDetailsResult
    suspend fun loadComments(postId: Int): CommentsResult
    suspend fun addComment(
        postId: Int,
        text: String,
        replyToCommentId: Int? = null,
        files: List<UploadFilePayload> = emptyList()
    ): ActionResult
    suspend fun deleteComment(commentId: Int): ActionResult
    suspend fun downloadImage(asset: PostImageAsset, preferLossless: Boolean = false): ByteArray?
    suspend fun downloadFileChunk(path: String, file: String, offset: Long): DownloadChunkResult
    suspend fun likePost(postId: Int): Boolean
    suspend fun dislikePost(postId: Int): Boolean
    suspend fun setReaction(postId: Int, reaction: String): Boolean
    suspend fun unsetReaction(postId: Int, reaction: String): Boolean
    suspend fun votePostPoll(postId: Int, optionIds: List<Int>): PollVoteResult
    suspend fun deletePost(postId: Int): ActionResult
    suspend fun restorePost(postId: Int): ActionResult
    suspend fun deletePostForever(postId: Int): ActionResult
    suspend fun addPostToArchive(postId: Int): ActionResult
    suspend fun removePostFromArchive(postId: Int): ActionResult
    suspend fun blockProfile(username: String): ActionResult
    suspend fun unblockProfile(username: String): ActionResult
}
