package elemsocial.com.domain.model

data class ProfileResult(
    val status: String,
    val message: String? = null,
    val profile: Profile? = null
) {
    val isSuccess: Boolean get() = status == "success" && profile != null
}

data class ProfilePostsResult(
    val status: String,
    val message: String? = null,
    val posts: List<FeedPost> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class ProfileGiftsResult(
    val status: String,
    val message: String? = null,
    val gifts: List<ProfileGift> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class ProfileCatalogGiftsResult(
    val status: String,
    val message: String? = null,
    val gifts: List<ProfileCatalogGift> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class ProfileRelationsResult(
    val status: String,
    val message: String? = null,
    val users: List<ProfileRelationUser> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class Profile(
    val id: Int,
    val type: String = "user",
    val name: String? = null,
    val username: String,
    val cover: PostImageAsset? = null,
    val avatar: PostImageAsset? = null,
    val description: String? = null,
    val createDate: String? = null,
    val lastOnline: String? = null,
    val icons: List<String> = emptyList(),
    val online: Boolean = false,
    val links: List<ProfileLink> = emptyList(),
    val deleted: Boolean = false,
    val blocked: Boolean = false,
    val subscribed: Boolean = false,
    val myProfile: Boolean = false,
    val stats: ProfileStats = ProfileStats()
)

data class ProfileStats(
    val subscriptions: Int = 0,
    val posts: Int = 0,
    val subscribers: Int = 0,
    val wallCount: Int = 0,
    val giftsCount: Int = 0
)

data class ProfileLink(
    val id: Int? = null,
    val title: String,
    val link: String
)

data class ProfileGift(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image: PostImageAsset? = null,
    val sender: ProfileGiftSender? = null,
    val price: Double? = null,
    val message: String? = null,
    val date: String? = null,
    val hidden: Boolean = false
)

data class ProfileCatalogGift(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image: PostImageAsset? = null,
    val price: Double = 0.0,
    val quantity: Int = 0
)

data class ProfileGiftSender(
    val name: String? = null,
    val username: String? = null,
    val avatar: PostImageAsset? = null
)

data class ProfileRelationUser(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: PostImageAsset? = null,
    val subscribers: Int = 0,
    val posts: Int = 0,
    val date: String? = null
)
