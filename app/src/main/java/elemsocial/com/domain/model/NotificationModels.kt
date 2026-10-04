package elemsocial.com.domain.model

data class NotificationsResult(
    val status: String,
    val message: String? = null,
    val notifications: List<AppNotification> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class AppNotification(
    val id: Int,
    val author: NotificationActor? = null,
    val contentAuthor: NotificationActor? = null,
    val action: String,
    val viewed: Boolean = false,
    val date: String? = null,
    val postId: Int? = null,
    val profileUsername: String? = null,
    val commentText: String? = null,
    val messageText: String? = null,
    val messageType: String? = null,
    val postText: String? = null,
    val amount: Double? = null,
    val subtype: String? = null,
    val title: String? = null,
    val message: String? = null,
    val endDate: String? = null,
    val durationHours: Int? = null
)

data class NotificationActor(
    val id: Int? = null,
    val type: Int? = null,
    val username: String? = null,
    val name: String? = null,
    val avatar: PostImageAsset? = null
)
