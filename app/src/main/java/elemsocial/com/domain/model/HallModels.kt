package elemsocial.com.domain.model

data class HallResult(
    val status: String,
    val message: String? = null,
    val users: List<HallUser> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class HallUser(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: PostImageAsset? = null,
    val icons: List<String> = emptyList(),
    val eballs: Double = 0.0
)
