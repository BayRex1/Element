package elemsocial.com.data.remote

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.domain.model.SearchCategory

class SocialRemoteDataSource(
    private val socketClient: ElementSocketClient
) {
    suspend fun getProfile(username: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "get_profile",
                "username" to username
            )
        )
    }

    suspend fun loadProfilePosts(
        authorId: Int,
        authorType: Int,
        startIndex: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "load_posts",
                "payload" to mapOf(
                    "posts_type" to "profile",
                    "author_id" to authorId,
                    "author_type" to authorType,
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun loadProfileWallPosts(
        username: String,
        startIndex: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "load_posts",
                "payload" to mapOf(
                    "posts_type" to "wall",
                    "username" to username,
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun subscribeProfile(username: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "profile/subscribe",
                "payload" to mapOf(
                    "username" to username
                )
            )
        )
    }

    suspend fun changeProfileName(name: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/name",
                "name" to name
            )
        )
    }

    suspend fun changeProfileDescription(description: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/description",
                "description" to description
            )
        )
    }

    suspend fun addProfileLink(title: String, link: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "add_link",
                "title" to title,
                "link" to link
            )
        )
    }

    suspend fun editProfileLink(linkId: Int, title: String, link: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "edit_link",
                "link_id" to linkId,
                "title" to title,
                "link" to link
            )
        )
    }

    suspend fun deleteProfileLink(linkId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "delete_link",
                "link_id" to linkId
            )
        )
    }

    suspend fun changeChannelName(channelId: Int, name: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/name",
                "payload" to mapOf(
                    "channel_id" to channelId,
                    "name" to name
                )
            )
        )
    }

    suspend fun changeChannelUsername(channelId: Int, username: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/username",
                "payload" to mapOf(
                    "channel_id" to channelId,
                    "username" to username
                )
            )
        )
    }

    suspend fun changeChannelDescription(channelId: Int, description: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/description",
                "payload" to mapOf(
                    "channel_id" to channelId,
                    "description" to description
                )
            )
        )
    }

    suspend fun uploadProfileAvatar(bytes: ByteArray): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/avatar/upload",
                "file" to bytes
            )
        )
    }

    suspend fun uploadProfileCover(bytes: ByteArray): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/cover/upload",
                "file" to bytes
            )
        )
    }

    suspend fun uploadChannelAvatar(channelId: Int, bytes: ByteArray): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/avatar/upload",
                "payload" to mapOf(
                    "channel_id" to channelId,
                    "file" to bytes
                )
            )
        )
    }

    suspend fun uploadChannelCover(channelId: Int, bytes: ByteArray): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/cover/upload",
                "payload" to mapOf(
                    "channel_id" to channelId,
                    "file" to bytes
                )
            )
        )
    }

    suspend fun deleteProfileAvatar(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/avatar/delete"
            )
        )
    }

    suspend fun deleteProfileCover(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/cover/delete"
            )
        )
    }

    suspend fun deleteChannelAvatar(channelId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/avatar/delete",
                "channel_id" to channelId
            )
        )
    }

    suspend fun deleteChannelCover(channelId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "channels/change/cover/delete",
                "channel_id" to channelId
            )
        )
    }

    suspend fun loadProfileGifts(username: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "gifts/load",
                "payload" to mapOf(
                    "username" to username
                )
            )
        )
    }

    suspend fun loadCatalogGifts(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "gifts/load"
            )
        )
    }

    suspend fun sendGift(
        username: String,
        giftId: Int,
        message: String?
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "gifts/send",
                "payload" to mapOf(
                    "username" to username,
                    "gift_id" to giftId,
                    "message" to message
                )
            )
        )
    }

    suspend fun loadProfileSubscriptions(
        username: String,
        startIndex: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "profile/load_subscriptions",
                "payload" to mapOf(
                    "username" to username,
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun loadProfileSubscribers(
        username: String,
        startIndex: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "profile/load_subscribers",
                "payload" to mapOf(
                    "username" to username,
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun setProfileGiftHidden(
        username: String,
        giftEntityId: Int,
        hidden: Boolean
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to if (hidden) "gifts/hide" else "gifts/show",
                "payload" to mapOf(
                    "username" to username,
                    "id" to giftEntityId
                )
            )
        )
    }

    suspend fun loadNotifications(startIndex: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "notifications/load",
                "payload" to mapOf(
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun viewNotifications(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "notifications/view"
            )
        )
    }

    suspend fun loadEballHistory(startIndex: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "eball/load_history",
                "payload" to mapOf(
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun loadEballHall(startIndex: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "eball/hall/load",
                "payload" to mapOf(
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun sendEball(
        recipientId: Int,
        amount: Double,
        message: String?
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "eball/send",
                "payload" to mapOf(
                    "recipient" to recipientId,
                    "amount" to amount,
                    "message" to message
                )
            )
        )
    }

    suspend fun goldPay(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "gold/pay"
            )
        )
    }

    suspend fun goldActivate(code: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "gold/activate",
                "code" to code
            )
        )
    }

    suspend fun loadReferralDashboard(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "referral/load"
            )
        )
    }

    suspend fun loadReferralHistory(
        startIndex: Int,
        limit: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "referral/history",
                "payload" to mapOf(
                    "start_index" to startIndex,
                    "limit" to limit
                )
            )
        )
    }

    suspend fun search(
        category: SearchCategory,
        value: String
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "search",
                "category" to category.apiValue,
                "value" to value
            )
        )
    }
}
