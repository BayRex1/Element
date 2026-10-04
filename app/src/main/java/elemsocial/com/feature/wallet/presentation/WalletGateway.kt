package elemsocial.com.feature.wallet.presentation

import elemsocial.com.domain.model.WalletHistoryResult
import elemsocial.com.domain.model.WalletGoldActionResult
import elemsocial.com.domain.model.WalletReferralDashboardResult
import elemsocial.com.domain.model.WalletReferralHistoryResult
import elemsocial.com.domain.model.WalletSendResult
import elemsocial.com.domain.model.WalletUser
import elemsocial.com.domain.repository.WalletRepository

class WalletGateway(
    private val walletRepository: WalletRepository
) {
    suspend fun loadHistory(startIndex: Int): WalletHistoryResult {
        return walletRepository.loadHistory(startIndex)
    }

    suspend fun sendEballs(recipientId: Int, amount: Double, message: String?): WalletSendResult {
        return walletRepository.sendEballs(recipientId, amount, message)
    }

    suspend fun searchUsers(query: String): List<WalletUser> {
        return walletRepository.searchUsers(query)
    }

    suspend fun goldPay(): WalletGoldActionResult {
        return walletRepository.goldPay()
    }

    suspend fun goldActivate(code: String): WalletGoldActionResult {
        return walletRepository.goldActivate(code)
    }

    suspend fun loadReferralDashboard(): WalletReferralDashboardResult {
        return walletRepository.loadReferralDashboard()
    }

    suspend fun loadReferralHistory(startIndex: Int, limit: Int = 25): WalletReferralHistoryResult {
        return walletRepository.loadReferralHistory(startIndex, limit)
    }
}
