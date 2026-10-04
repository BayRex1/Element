package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.SocialRemoteDataSource
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.ProfileCatalogGift
import elemsocial.com.domain.model.ProfileCatalogGiftsResult
import elemsocial.com.domain.model.Profile
import elemsocial.com.domain.model.ProfileGift
import elemsocial.com.domain.model.ProfileGiftSender
import elemsocial.com.domain.model.ProfileGiftsResult
import elemsocial.com.domain.model.ProfileLink
import elemsocial.com.domain.model.ProfilePostsResult
import elemsocial.com.domain.model.ProfileRelationUser
import elemsocial.com.domain.model.ProfileRelationsResult
import elemsocial.com.domain.model.ProfileResult
import elemsocial.com.domain.model.ProfileStats
import elemsocial.com.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    socketClient: ElementSocketClient
) : ProfileRepository {
    private val remote = SocialRemoteDataSource(socketClient)

    override suspend fun loadProfile(username: String): ProfileResult {
        val response = remote.getProfile(username)
        val status = response["status"]?.toString() ?: "error"
        val message = response["message"]?.toString()
        val data = response["data"].asRichMap()

        return ProfileResult(
            status = status,
            message = message,
            profile = data?.toProfile()
        )
    }

    override suspend fun loadProfilePosts(
        authorId: Int,
        authorType: Int,
        startIndex: Int
    ): ProfilePostsResult {
        val response = remote.loadProfilePosts(
            authorId = authorId,
            authorType = authorType,
            startIndex = startIndex
        )
        val status = response["status"]?.toString()
            ?: if (response.containsKey("posts")) "success" else "error"

        return ProfilePostsResult(
            status = status,
            message = response["message"]?.toString(),
            posts = parseFeedPosts(response["posts"])
        )
    }

    override suspend fun loadProfileWallPosts(
        username: String,
        startIndex: Int
    ): ProfilePostsResult {
        val response = remote.loadProfileWallPosts(
            username = username,
            startIndex = startIndex
        )
        val status = response["status"]?.toString()
            ?: if (response.containsKey("posts")) "success" else "error"

        return ProfilePostsResult(
            status = status,
            message = response["message"]?.toString(),
            posts = parseFeedPosts(response["posts"])
        )
    }

    override suspend fun loadProfileGifts(username: String): ProfileGiftsResult {
        val response = remote.loadProfileGifts(username)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("gifts")) "success" else "error"
        val giftsRaw = response["gifts"] as? List<*> ?: emptyList<Any?>()

        return ProfileGiftsResult(
            status = status,
            message = response["message"]?.toString(),
            gifts = giftsRaw.mapNotNull { it.toProfileGift() }
        )
    }

    override suspend fun loadCatalogGifts(): ProfileCatalogGiftsResult {
        val response = remote.loadCatalogGifts()
        val status = response["status"]?.toString()
            ?: if (response.containsKey("gifts")) "success" else "error"
        val giftsRaw = response["gifts"] as? List<*> ?: emptyList<Any?>()

        return ProfileCatalogGiftsResult(
            status = status,
            message = response["message"]?.toString(),
            gifts = giftsRaw.mapNotNull { it.toProfileCatalogGift() }
        )
    }

    override suspend fun sendGift(username: String, giftId: Int, message: String?): ActionResult {
        val response = remote.sendGift(
            username = username,
            giftId = giftId,
            message = message
        )
        return actionResult(response)
    }

    override suspend fun loadProfileSubscriptions(
        username: String,
        startIndex: Int
    ): ProfileRelationsResult {
        val response = remote.loadProfileSubscriptions(username, startIndex)
        return profileRelationsResult(response)
    }

    override suspend fun loadProfileSubscribers(
        username: String,
        startIndex: Int
    ): ProfileRelationsResult {
        val response = remote.loadProfileSubscribers(username, startIndex)
        return profileRelationsResult(response)
    }

    override suspend fun setProfileGiftHidden(
        username: String,
        giftEntityId: Int,
        hidden: Boolean
    ): ActionResult {
        val response = remote.setProfileGiftHidden(
            username = username,
            giftEntityId = giftEntityId,
            hidden = hidden
        )
        return actionResult(response)
    }

    override suspend fun subscribeProfile(username: String): ActionResult {
        val response = remote.subscribeProfile(username)
        return actionResult(response)
    }

    override suspend fun changeProfileName(name: String): ActionResult {
        val response = remote.changeProfileName(name)
        return actionResult(response)
    }

    override suspend fun changeProfileDescription(description: String): ActionResult {
        val response = remote.changeProfileDescription(description)
        return actionResult(response)
    }

    override suspend fun addProfileLink(title: String, link: String): ActionResult {
        val response = remote.addProfileLink(title, link)
        return actionResult(response)
    }

    override suspend fun editProfileLink(linkId: Int, title: String, link: String): ActionResult {
        val response = remote.editProfileLink(linkId, title, link)
        return actionResult(response)
    }

    override suspend fun deleteProfileLink(linkId: Int): ActionResult {
        val response = remote.deleteProfileLink(linkId)
        return actionResult(response)
    }

    override suspend fun changeChannelName(channelId: Int, name: String): ActionResult {
        val response = remote.changeChannelName(channelId, name)
        return actionResult(response)
    }

    override suspend fun changeChannelUsername(channelId: Int, username: String): ActionResult {
        val response = remote.changeChannelUsername(channelId, username)
        return actionResult(response)
    }

    override suspend fun changeChannelDescription(channelId: Int, description: String): ActionResult {
        val response = remote.changeChannelDescription(channelId, description)
        return actionResult(response)
    }

    override suspend fun uploadProfileAvatar(bytes: ByteArray): ActionResult {
        val response = remote.uploadProfileAvatar(bytes)
        return actionResult(response)
    }

    override suspend fun uploadProfileCover(bytes: ByteArray): ActionResult {
        val response = remote.uploadProfileCover(bytes)
        return actionResult(response)
    }

    override suspend fun uploadChannelAvatar(channelId: Int, bytes: ByteArray): ActionResult {
        val response = remote.uploadChannelAvatar(channelId, bytes)
        return actionResult(response)
    }

    override suspend fun uploadChannelCover(channelId: Int, bytes: ByteArray): ActionResult {
        val response = remote.uploadChannelCover(channelId, bytes)
        return actionResult(response)
    }

    override suspend fun deleteProfileAvatar(): ActionResult {
        val response = remote.deleteProfileAvatar()
        return actionResult(response)
    }

    override suspend fun deleteProfileCover(): ActionResult {
        val response = remote.deleteProfileCover()
        return actionResult(response)
    }

    override suspend fun deleteChannelAvatar(channelId: Int): ActionResult {
        val response = remote.deleteChannelAvatar(channelId)
        return actionResult(response)
    }

    override suspend fun deleteChannelCover(channelId: Int): ActionResult {
        val response = remote.deleteChannelCover(channelId)
        return actionResult(response)
    }

    private fun profileRelationsResult(response: Map<String, Any?>): ProfileRelationsResult {
        val status = response["status"]?.toString()
            ?: if (response.containsKey("users")) "success" else "error"
        val usersRaw = response["users"] as? List<*> ?: emptyList<Any?>()

        return ProfileRelationsResult(
            status = status,
            message = response["message"]?.toString(),
            users = usersRaw.mapNotNull { it.toProfileRelationUser() }
        )
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

private fun Map<String, Any?>.toProfile(): Profile? {
    val id = this["id"].asInt() ?: return null
    val username = this["username"]?.toString()?.takeIf { it.isNotBlank() } ?: return null

    return Profile(
        id = id,
        type = this["type"]?.toString() ?: "user",
        name = this["name"]?.toString(),
        username = username,
        cover = parseAsset(this["cover"]),
        avatar = parseAsset(this["avatar"]),
        description = this["description"]?.toString(),
        createDate = this["create_date"]?.toString(),
        lastOnline = this["last_online"]?.toString(),
        icons = parseProfileIcons(this["icons"]),
        online = this["online"].asBoolean(),
        links = parseProfileLinks(this["links"]),
        deleted = this["deleted"].asBoolean(),
        blocked = this["blocked"].asBoolean(),
        subscribed = this["subscribed"].asBoolean(),
        myProfile = this["my_profile"].asBoolean(),
        stats = ProfileStats(
            subscriptions = this["subscriptions"].asInt(0) ?: 0,
            posts = this["posts"].asInt(0) ?: 0,
            subscribers = this["subscribers"].asInt(0) ?: 0,
            wallCount = this["wall_count"].asInt(0) ?: 0,
            giftsCount = this["gifts_count"].asInt(0) ?: 0
        )
    )
}

private fun parseProfileIcons(raw: Any?): List<String> {
    return when (raw) {
        is List<*> -> raw.mapNotNull { item ->
            item.asMap()?.get("icon_id")?.toString() ?: item?.toString()
        }

        is String -> listOf(raw)
        else -> emptyList()
    }
}

private fun parseProfileLinks(raw: Any?): List<ProfileLink> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        val map = item.asMap() ?: return@mapNotNull null
        val link = (map["link"] ?: map["url"])?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val title = map["title"]?.toString()?.takeIf { it.isNotBlank() } ?: link
        ProfileLink(
            id = map["id"].asInt(),
            title = title,
            link = link
        )
    }
}

private fun Any?.toProfileGift(): ProfileGift? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: "Подарок"

    return ProfileGift(
        id = id,
        name = name,
        description = map["description"]?.toString(),
        image = parseAsset(map["image"]),
        sender = map["sender"].asRichMap()?.let { sender ->
            ProfileGiftSender(
                name = sender["name"]?.toString(),
                username = sender["username"]?.toString(),
                avatar = parseAsset(sender["avatar"])
            )
        },
        price = map["price"].asDouble(),
        message = map["message"]?.toString(),
        date = map["date"]?.toString(),
        hidden = map["is_hidden"].asBoolean()
    )
}

private fun Any?.toProfileRelationUser(): ProfileRelationUser? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val username = map["username"]?.toString()?.takeIf { it.isNotBlank() } ?: return null

    return ProfileRelationUser(
        id = id,
        name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: username,
        username = username,
        avatar = parseAsset(map["avatar"]),
        subscribers = map["subscribers"].asInt(0) ?: 0,
        posts = map["posts"].asInt(0) ?: 0,
        date = map["date"]?.toString()
    )
}

private fun Any?.toProfileCatalogGift(): ProfileCatalogGift? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return null

    return ProfileCatalogGift(
        id = id,
        name = name,
        description = map["description"]?.toString(),
        image = parseAsset(map["image"]),
        price = map["price"].asDouble(0.0) ?: 0.0,
        quantity = map["quantity"].asInt(0) ?: 0
    )
}
