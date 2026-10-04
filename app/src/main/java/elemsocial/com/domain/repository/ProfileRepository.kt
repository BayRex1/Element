package elemsocial.com.domain.repository

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.ProfileCatalogGiftsResult
import elemsocial.com.domain.model.ProfileGiftsResult
import elemsocial.com.domain.model.ProfilePostsResult
import elemsocial.com.domain.model.ProfileRelationsResult
import elemsocial.com.domain.model.ProfileResult

interface ProfileRepository {
    suspend fun loadProfile(username: String): ProfileResult
    suspend fun loadProfilePosts(authorId: Int, authorType: Int, startIndex: Int): ProfilePostsResult
    suspend fun loadProfileWallPosts(username: String, startIndex: Int): ProfilePostsResult
    suspend fun loadProfileGifts(username: String): ProfileGiftsResult
    suspend fun loadCatalogGifts(): ProfileCatalogGiftsResult
    suspend fun sendGift(username: String, giftId: Int, message: String?): ActionResult
    suspend fun loadProfileSubscriptions(username: String, startIndex: Int): ProfileRelationsResult
    suspend fun loadProfileSubscribers(username: String, startIndex: Int): ProfileRelationsResult
    suspend fun setProfileGiftHidden(username: String, giftEntityId: Int, hidden: Boolean): ActionResult
    suspend fun subscribeProfile(username: String): ActionResult
    suspend fun changeProfileName(name: String): ActionResult
    suspend fun changeProfileDescription(description: String): ActionResult
    suspend fun addProfileLink(title: String, link: String): ActionResult
    suspend fun editProfileLink(linkId: Int, title: String, link: String): ActionResult
    suspend fun deleteProfileLink(linkId: Int): ActionResult
    suspend fun changeChannelName(channelId: Int, name: String): ActionResult
    suspend fun changeChannelUsername(channelId: Int, username: String): ActionResult
    suspend fun changeChannelDescription(channelId: Int, description: String): ActionResult
    suspend fun uploadProfileAvatar(bytes: ByteArray): ActionResult
    suspend fun uploadProfileCover(bytes: ByteArray): ActionResult
    suspend fun uploadChannelAvatar(channelId: Int, bytes: ByteArray): ActionResult
    suspend fun uploadChannelCover(channelId: Int, bytes: ByteArray): ActionResult
    suspend fun deleteProfileAvatar(): ActionResult
    suspend fun deleteProfileCover(): ActionResult
    suspend fun deleteChannelAvatar(channelId: Int): ActionResult
    suspend fun deleteChannelCover(channelId: Int): ActionResult
}
