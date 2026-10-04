package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.SocialRemoteDataSource
import elemsocial.com.domain.model.WalletGift
import elemsocial.com.domain.model.WalletGoldActionResult
import elemsocial.com.domain.model.WalletHistoryResult
import elemsocial.com.domain.model.WalletReferralDashboard
import elemsocial.com.domain.model.WalletReferralDashboardResult
import elemsocial.com.domain.model.WalletReferralHistoryItem
import elemsocial.com.domain.model.WalletReferralHistoryResult
import elemsocial.com.domain.model.WalletSendResult
import elemsocial.com.domain.model.WalletTransaction
import elemsocial.com.domain.model.WalletUser
import elemsocial.com.domain.repository.WalletRepository

class WalletRepositoryImpl(
    socketClient: ElementSocketClient
) : WalletRepository {
    private val remote = SocialRemoteDataSource(socketClient)

    override suspend fun loadHistory(startIndex: Int): WalletHistoryResult {
        val response = remote.loadEballHistory(startIndex)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("transactions")) "success" else "error"
        val transactionsRaw = response["transactions"] as? List<*> ?: emptyList<Any?>()

        return WalletHistoryResult(
            status = status,
            message = response["message"]?.toString(),
            transactions = transactionsRaw.mapNotNull { it.toWalletTransaction() }
        )
    }

    override suspend fun sendEballs(
        recipientId: Int,
        amount: Double,
        message: String?
    ): WalletSendResult {
        val response = remote.sendEball(
            recipientId = recipientId,
            amount = amount,
            message = message
        )
        val status = response["status"]?.toString() ?: "error"
        val data = response["data"].asRichMap()
        return WalletSendResult(
            status = status,
            message = response["message"]?.toString(),
            sentAmount = data?.get("sent_amount").asDouble(),
            commissionAmount = data?.get("commission_amount").asDouble(),
            totalDeducted = data?.get("total_deducted").asDouble()
        )
    }

    override suspend fun searchUsers(query: String): List<WalletUser> {
        val response = remote.search(
            category = elemsocial.com.domain.model.SearchCategory.Users,
            value = query
        )
        if (response["status"]?.toString() != "success") return emptyList()
        val list = response["results"] as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item.asRichMap() ?: return@mapNotNull null
            val id = map["id"].asInt() ?: return@mapNotNull null
            val username = map["username"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            WalletUser(
                id = id,
                username = username,
                name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: username,
                avatar = parseAsset(map["avatar"])
            )
        }
    }

    override suspend fun goldPay(): WalletGoldActionResult {
        val response = remote.goldPay()
        return WalletGoldActionResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString()
        )
    }

    override suspend fun goldActivate(code: String): WalletGoldActionResult {
        val response = remote.goldActivate(code)
        return WalletGoldActionResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString()
        )
    }

    override suspend fun loadReferralDashboard(): WalletReferralDashboardResult {
        val response = remote.loadReferralDashboard()
        val status = response["status"]?.toString()
            ?: if (response.containsKey("profile")) "success" else "error"
        val profile = response["profile"].asRichMap()
        val stats = response["stats"].asRichMap()
        val invitedBy = response["invited_by"].asRichMap()
        val refCode = profile?.get("ref_code")?.toString().orEmpty()
        val inviteLink = profile?.get("invite_link")?.toString().orEmpty()

        val dashboard = if (refCode.isNotBlank() || inviteLink.isNotBlank()) {
            WalletReferralDashboard(
                refCode = refCode,
                inviteLink = inviteLink,
                totalInvited = stats?.get("total_invited").asInt(0) ?: 0,
                rewarded = stats?.get("rewarded").asInt(0) ?: 0,
                pending = stats?.get("pending").asInt(0) ?: 0,
                rejected = stats?.get("rejected").asInt(0) ?: 0,
                totalEarned = stats?.get("total_earned").asDouble(0.0) ?: 0.0,
                invitedByUsername = invitedBy?.get("inviter_username")?.toString()
            )
        } else {
            null
        }

        return WalletReferralDashboardResult(
            status = status,
            message = response["message"]?.toString(),
            dashboard = dashboard
        )
    }

    override suspend fun loadReferralHistory(startIndex: Int, limit: Int): WalletReferralHistoryResult {
        val response = remote.loadReferralHistory(startIndex, limit)
        val status = response["status"]?.toString()
            ?: if (response.containsKey("history")) "success" else "error"
        val rows = response["history"] as? List<*> ?: emptyList<Any?>()
        return WalletReferralHistoryResult(
            status = status,
            message = response["message"]?.toString(),
            history = rows.mapNotNull { it.toReferralHistoryItem() }
        )
    }
}

private fun Any?.toWalletUser(): WalletUser? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val username = map["username"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    return WalletUser(
        id = id,
        username = username,
        name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: username,
        avatar = parseAsset(map["avatar"])
    )
}

private fun Any?.toWalletGift(): WalletGift? {
    val map = this.asRichMap() ?: return null
    return WalletGift(
        id = map["id"].asInt(),
        name = map["name"]?.toString(),
        image = parseAsset(map["image"])
    )
}

private fun Any?.toWalletTransaction(): WalletTransaction? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val type = map["type"]?.toString() ?: "transfer"
    return WalletTransaction(
        id = id,
        type = type,
        sender = map["sender"].toWalletUser(),
        recipient = map["recipient"].toWalletUser(),
        amount = map["amount"].asDouble(0.0) ?: 0.0,
        fee = map["fee"].asDouble(0.0) ?: 0.0,
        message = map["message"]?.toString(),
        date = map["date"]?.toString().orEmpty(),
        incoming = map["is_incoming"].asBoolean(),
        gift = map["gift"].toWalletGift(),
        giftRecipient = map["gift_recipient"].toWalletUser()
    )
}

private fun Any?.toReferralHistoryItem(): WalletReferralHistoryItem? {
    val map = this.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val rewards = map["rewards"].asRichMap()
    return WalletReferralHistoryItem(
        id = id,
        invited = map["invited"].toWalletUser(),
        status = map["status"]?.toString().orEmpty(),
        rewardInviter = rewards?.get("inviter").asDouble(0.0) ?: 0.0,
        createdAt = map["created_at"]?.toString(),
        rewardDate = map["reward_date"]?.toString()
    )
}
