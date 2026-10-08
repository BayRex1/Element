package elemsocial.com.core.plugins.sdk

import elemsocial.com.core.plugins.ElementPluginHostV2
import elemsocial.com.core.plugins.ElementPluginServerResponse

class ElementPluginServerApi(private val host: ElementPluginHostV2) {
    suspend fun raw(type: String, action: String, payload: Map<String, Any?> = emptyMap()): ElementPluginServerResponse =
        host.serverRequest(type, action, payload)

    suspend fun loadPost(postId: Int): ElementPluginServerResponse =
        raw("social", "load_post", mapOf("post_id" to postId))

    suspend fun loadComments(postId: Int): ElementPluginServerResponse =
        raw("social", "comments/load", mapOf("post_id" to postId))

    suspend fun createPost(text: String): ElementPluginServerResponse =
        raw("social", "posts/add", mapOf("text" to text, "files" to emptyList<Any>(), "songs" to emptyList<Int>()))

    suspend fun editPost(postId: Int, text: String): ElementPluginServerResponse =
        raw("social", "posts/edit", mapOf("post_id" to postId, "text" to text))

    suspend fun deletePost(postId: Int): ElementPluginServerResponse =
        raw("social", "posts/delete", mapOf("post_id" to postId))

    suspend fun likePost(postId: Int): ElementPluginServerResponse =
        raw("social", "posts/like", mapOf("post_id" to postId))

    suspend fun dislikePost(postId: Int): ElementPluginServerResponse =
        raw("social", "posts/dislike", mapOf("post_id" to postId))

    suspend fun react(postId: Int, reaction: String): ElementPluginServerResponse =
        raw("social", "post/set_reaction", mapOf("post_id" to postId, "reaction" to reaction))

    suspend fun comment(postId: Int, text: String): ElementPluginServerResponse =
        raw("social", "comments/add", mapOf("post_id" to postId, "text" to text, "files" to emptyList<Any>()))
}
