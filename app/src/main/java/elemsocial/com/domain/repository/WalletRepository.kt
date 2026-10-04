package elemsocial.com.domain.repository

import elemsocial.com.domain.model.WalletHistoryResult
import elemsocial.com.domain.model.WalletGoldActionResult
import elemsocial.com.domain.model.WalletReferralDashboardResult
import elemsocial.com.domain.model.WalletReferralHistoryResult
import elemsocial.com.domain.model.WalletSendResult
import elemsocial.com.domain.model.WalletUser

interface WalletRepository {
    suspend fun loadHistory(startIndex: Int): WalletHistoryResult
    suspend fun sendEballs(recipientId: Int, amount: Double, message: String?): WalletSendResult
    suspend fun searchUsers(query: String): List<WalletUser>
    suspend fun goldPay(): WalletGoldActionResult
    suspend fun goldActivate(code: String): WalletGoldActionResult
    suspend fun loadReferralDashboard(): WalletReferralDashboardResult
    suspend fun loadReferralHistory(startIndex: Int, limit: Int = 25): WalletReferralHistoryResult
}
