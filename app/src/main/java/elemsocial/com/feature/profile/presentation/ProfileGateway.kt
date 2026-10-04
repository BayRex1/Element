package elemsocial.com.feature.profile.presentation

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.ProfileCatalogGiftsResult
import elemsocial.com.domain.model.ProfileGiftsResult
import elemsocial.com.domain.model.ProfilePostsResult
import elemsocial.com.domain.model.ProfileRelationsResult
import elemsocial.com.domain.model.ProfileResult
import elemsocial.com.domain.repository.ProfileRepository

class ProfileGateway(
    private val profileRepository: ProfileRepository
) {
    suspend fun loadProfile(username: String): ProfileResult {
        return profileRepository.loadProfile(username)
    }

    suspend fun loadProfilePosts(authorId: Int, authorType: Int, startIndex: Int): ProfilePostsResult {
        return profileRepository.loadProfilePosts(
            authorId = authorId,
            authorType = authorType,
            startIndex = startIndex
        )
    }

    suspend fun loadProfileWallPosts(username: String, startIndex: Int): ProfilePostsResult {
        return profileRepository.loadProfileWallPosts(
            username = username,
            startIndex = startIndex
        )
    }

    suspend fun loadProfileGifts(username: String): ProfileGiftsResult {
        return profileRepository.loadProfileGifts(username)
    }

    suspend fun loadCatalogGifts(): ProfileCatalogGiftsResult {
        return profileRepository.loadCatalogGifts()
    }

    suspend fun sendGift(username: String, giftId: Int, message: String?): ActionResult {
        return profileRepository.sendGift(username, giftId, message)
    }

    suspend fun loadProfileSubscriptions(username: String, startIndex: Int): ProfileRelationsResult {
        return profileRepository.loadProfileSubscriptions(username, startIndex)
    }

    suspend fun loadProfileSubscribers(username: String, startIndex: Int): ProfileRelationsResult {
        return profileRepository.loadProfileSubscribers(username, startIndex)
    }

    suspend fun setProfileGiftHidden(
        username: String,
        giftEntityId: Int,
        hidden: Boolean
    ): ActionResult {
        return profileRepository.setProfileGiftHidden(
            username = username,
            giftEntityId = giftEntityId,
            hidden = hidden
        )
    }

    suspend fun subscribeProfile(username: String): ActionResult {
        return profileRepository.subscribeProfile(username)
    }

    suspend fun changeProfileName(name: String): ActionResult {
        return profileRepository.changeProfileName(name)
    }

    suspend fun changeProfileDescription(description: String): ActionResult {
        return profileRepository.changeProfileDescription(description)
    }

    suspend fun addProfileLink(title: String, link: String): ActionResult {
        return profileRepository.addProfileLink(title, link)
    }

    suspend fun editProfileLink(linkId: Int, title: String, link: String): ActionResult {
        return profileRepository.editProfileLink(linkId, title, link)
    }

    suspend fun deleteProfileLink(linkId: Int): ActionResult {
        return profileRepository.deleteProfileLink(linkId)
    }

    suspend fun changeChannelName(channelId: Int, name: String): ActionResult {
        return profileRepository.changeChannelName(channelId, name)
    }

    suspend fun changeChannelUsername(channelId: Int, username: String): ActionResult {
        return profileRepository.changeChannelUsername(channelId, username)
    }

    suspend fun changeChannelDescription(channelId: Int, description: String): ActionResult {
        return profileRepository.changeChannelDescription(channelId, description)
    }

    suspend fun uploadProfileAvatar(bytes: ByteArray): ActionResult {
        return profileRepository.uploadProfileAvatar(bytes)
    }

    suspend fun uploadProfileCover(bytes: ByteArray): ActionResult {
        return profileRepository.uploadProfileCover(bytes)
    }

    suspend fun uploadChannelAvatar(channelId: Int, bytes: ByteArray): ActionResult {
        return profileRepository.uploadChannelAvatar(channelId, bytes)
    }

    suspend fun uploadChannelCover(channelId: Int, bytes: ByteArray): ActionResult {
        return profileRepository.uploadChannelCover(channelId, bytes)
    }

    suspend fun deleteProfileAvatar(): ActionResult {
        return profileRepository.deleteProfileAvatar()
    }

    suspend fun deleteProfileCover(): ActionResult {
        return profileRepository.deleteProfileCover()
    }

    suspend fun deleteChannelAvatar(channelId: Int): ActionResult {
        return profileRepository.deleteChannelAvatar(channelId)
    }

    suspend fun deleteChannelCover(channelId: Int): ActionResult {
        return profileRepository.deleteChannelCover(channelId)
    }
}
