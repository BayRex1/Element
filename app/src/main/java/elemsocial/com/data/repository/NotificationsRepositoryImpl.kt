package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.SocialRemoteDataSource
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.AppNotification
import elemsocial.com.domain.model.NotificationActor
import elemsocial.com.domain.model.NotificationsResult
import elemsocial.com.domain.repository.NotificationsRepository

class NotificationsRepositoryImpl(
    socketClient: ElementSocketClient
) : NotificationsRepository {
    private val remote = SocialRemoteDataSource(socketClient)

    override suspend fun loadNotifications(startIndex: Int): NotificationsResult {
        val response = remote.loadNotifications(startIndex)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("notifications")) "success" else "error"

        return NotificationsResult(
            status = status,
            message = response["message"]?.toString(),
            notifications = parseNotifications(response["notifications"])
        )
    }

    override suspend fun viewNotifications(): ActionResult {
        return actionResult(remote.viewNotifications())
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
}

internal fun parseNotifications(raw: Any?): List<AppNotification> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull(::parseNotification)
}

internal fun parseNotification(raw: Any?): AppNotification? {
    val map = raw.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val action = map["action"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    val content = map["content"].asRichMap() ?: emptyMap()
    val post = content["post"].asRichMap()
    val profile = content["profile"].asRichMap()
    val comment = content["comment"].asRichMap()
    val message = content["message"].asRichMap()
    val data = content["data"].asRichMap()

    return AppNotification(
        id = id,
        author = parseNotificationActor(map["author"]),
        contentAuthor = parseNotificationActor(content["author"]),
        action = action,
        viewed = map["viewed"].asBoolean(),
        date = map["date"]?.toString(),
        postId = post?.get("id").asInt(),
        profileUsername = profile?.get("username")?.toString(),
        commentText = comment?.get("text")?.toString(),
        messageText = message?.get("text")?.toString(),
        messageType = message?.get("type")?.toString(),
        postText = post?.get("text")?.toString(),
        amount = content["amount"].asDouble(),
        subtype = content["subtype"]?.toString(),
        title = content["title"]?.toString(),
        message = (content["message"] as? String)?.takeIf { it.isNotBlank() },
        endDate = data?.get("end_date")?.toString(),
        durationHours = data?.get("duration_hours").asInt()
    )
}

private fun parseNotificationActor(raw: Any?): NotificationActor? {
    val map = raw.asRichMap() ?: return null
    return NotificationActor(
        id = map["id"].asInt(),
        type = map["type"].asInt(),
        username = map["username"]?.toString(),
        name = map["name"]?.toString(),
        avatar = parseAsset(map["avatar"])
    )
}
