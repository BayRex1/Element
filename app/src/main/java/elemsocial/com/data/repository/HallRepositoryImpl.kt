package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.SocialRemoteDataSource
import elemsocial.com.domain.model.HallResult
import elemsocial.com.domain.model.HallUser
import elemsocial.com.domain.repository.HallRepository

class HallRepositoryImpl(
    socketClient: ElementSocketClient
) : HallRepository {
    private val remote = SocialRemoteDataSource(socketClient)

    override suspend fun loadHall(startIndex: Int): HallResult {
        val response = remote.loadEballHall(startIndex)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("users")) "success" else "error"
        val usersRaw = response["users"] as? List<*> ?: emptyList<Any?>()
        return HallResult(
            status = status,
            message = response["message"]?.toString(),
            users = usersRaw.mapNotNull { it.toHallUser() }
        )
    }
}

private fun Any?.toHallUser(): HallUser? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val username = map["username"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    return HallUser(
        id = id,
        username = username,
        name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: username,
        avatar = parseAsset(map["avatar"]),
        icons = parseHallIcons(map["icons"]),
        eballs = map["eballs"].asDouble(0.0) ?: 0.0
    )
}

private fun parseHallIcons(raw: Any?): List<String> {
    return when (raw) {
        is List<*> -> raw.mapNotNull { item ->
            item.asMap()?.get("icon_id")?.toString() ?: item?.toString()
        }

        is String -> listOf(raw)
        else -> emptyList()
    }
}
