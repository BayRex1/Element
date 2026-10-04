package elemsocial.com.domain.model

data class WalletHistoryResult(
    val status: String,
    val message: String? = null,
    val transactions: List<WalletTransaction> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class WalletGoldActionResult(
    val status: String,
    val message: String? = null
) {
    val isSuccess: Boolean get() = status == "success"
}

data class WalletSendResult(
    val status: String,
    val message: String? = null,
    val sentAmount: Double? = null,
    val commissionAmount: Double? = null,
    val totalDeducted: Double? = null
) {
    val isSuccess: Boolean get() = status == "success"
}

data class WalletUser(
    val id: Int,
    val username: String,
    val name: String,
    val avatar: PostImageAsset? = null
)

data class WalletReferralDashboardResult(
    val status: String,
    val message: String? = null,
    val dashboard: WalletReferralDashboard? = null
) {
    val isSuccess: Boolean get() = status == "success" && dashboard != null
}

data class WalletReferralHistoryResult(
    val status: String,
    val message: String? = null,
    val history: List<WalletReferralHistoryItem> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}

data class WalletGift(
    val id: Int? = null,
    val name: String? = null,
    val image: PostImageAsset? = null
)

data class WalletReferralDashboard(
    val refCode: String,
    val inviteLink: String,
    val totalInvited: Int = 0,
    val rewarded: Int = 0,
    val pending: Int = 0,
    val rejected: Int = 0,
    val totalEarned: Double = 0.0,
    val invitedByUsername: String? = null
)

data class WalletReferralHistoryItem(
    val id: Int,
    val invited: WalletUser? = null,
    val status: String = "",
    val rewardInviter: Double = 0.0,
    val createdAt: String? = null,
    val rewardDate: String? = null
)

data class WalletTransaction(
    val id: Int,
    val type: String,
    val sender: WalletUser? = null,
    val recipient: WalletUser? = null,
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val message: String? = null,
    val date: String = "",
    val incoming: Boolean = false,
    val gift: WalletGift? = null,
    val giftRecipient: WalletUser? = null
)
