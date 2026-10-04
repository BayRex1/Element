package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.SocialRemoteDataSource
import elemsocial.com.domain.model.SearchCategory
import elemsocial.com.domain.model.SearchResult
import elemsocial.com.domain.model.SearchResultItem
import elemsocial.com.domain.repository.SearchRepository

class SearchRepositoryImpl(
    socketClient: ElementSocketClient
) : SearchRepository {
    private val remote = SocialRemoteDataSource(socketClient)

    override suspend fun search(category: SearchCategory, value: String): SearchResult {
        val response = remote.search(
            category = category,
            value = value
        )

        return SearchResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            results = parseSearchResults(response["results"])
        )
    }
}

private fun parseSearchResults(raw: Any?): List<SearchResultItem> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        val map = item.asRichMap() ?: return@mapNotNull null
        when (map["type"]?.toString()) {
            "user", "channel" -> {
                val id = map["id"].asInt() ?: return@mapNotNull null
                val username = map["username"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SearchResultItem.User(
                    id = id,
                    type = map["type"]?.toString() ?: "user",
                    username = username,
                    name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: username,
                    avatar = parseAsset(map["avatar"]),
                    subscribers = map["subscribers"].asInt(0) ?: 0,
                    posts = map["posts"].asInt(0) ?: 0
                )
            }

            "post" -> {
                val id = map["id"].asInt() ?: return@mapNotNull null
                val author = map["author"].asRichMap()
                SearchResultItem.Post(
                    id = id,
                    text = map["text"]?.toString(),
                    authorName = author?.get("name")?.toString() ?: "Пользователь",
                    authorUsername = author?.get("username")?.toString() ?: "unknown",
                    authorAvatar = parseAsset(author?.get("avatar"))
                )
            }

            "song" -> {
                val id = map["id"].asInt() ?: return@mapNotNull null
                val title = map["title"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artist = map["artist"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SearchResultItem.Music(
                    id = id,
                    title = title,
                    artist = artist,
                    cover = parseAsset(map["cover"]),
                    durationSeconds = map["duration"].asDouble()
                )
            }

            else -> null
        }
    }
}
