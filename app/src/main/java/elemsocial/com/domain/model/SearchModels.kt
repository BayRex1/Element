package elemsocial.com.domain.model

enum class SearchCategory(
    val apiValue: String,
    val title: String
) {
    Users("users", "Люди"),
    Posts("posts", "Посты"),
    Music("music", "Музыка")
}

data class SearchResult(
    val status: String,
    val message: String? = null,
    val results: List<SearchResultItem> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

sealed class SearchResultItem {
    data class User(
        val id: Int,
        val type: String,
        val username: String,
        val name: String,
        val avatar: PostImageAsset? = null,
        val subscribers: Int = 0,
        val posts: Int = 0
    ) : SearchResultItem()

    data class Post(
        val id: Int,
        val text: String? = null,
        val authorName: String = "Пользователь",
        val authorUsername: String = "unknown",
        val authorAvatar: PostImageAsset? = null
    ) : SearchResultItem()

    data class Music(
        val id: Int,
        val title: String,
        val artist: String,
        val cover: PostImageAsset? = null,
        val durationSeconds: Double? = null
    ) : SearchResultItem()
}
