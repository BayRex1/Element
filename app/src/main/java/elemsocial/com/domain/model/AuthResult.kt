package elemsocial.com.domain.model

data class AuthResult(
    val status: String,
    val message: String? = null,
    val method: String? = null,
    val email: String? = null,
    val sessionKey: String? = null,
    val accountId: Int? = null,
    val accountName: String? = null,
    val accountUsername: String? = null,
    val accountAvatar: PostImageAsset? = null,
    val accountBalance: Double? = null,
    val goldStatus: Boolean = false,
    val goldHistory: List<AuthGoldHistory> = emptyList(),
    val accountChannels: List<AuthAccountChannel> = emptyList(),
    val notificationsCount: Int = 0,
    val isAdmin: Boolean = false,
    val raw: Map<String, Any?> = emptyMap()
) {
    val isSuccess: Boolean get() = status.equals("success", ignoreCase = true)
    val hasAccountIdentity: Boolean
        get() = accountId != null || !accountUsername.isNullOrBlank()
    val isAccountNotFound: Boolean
        get() {
            val messageText = buildString {
                append(message.orEmpty())
                append(' ')
                append(raw["error"]?.toString().orEmpty())
            }.lowercase()
            return messageText.contains("account not found") ||
                messageText.contains("аккаунт не найден") ||
                messageText.contains("профиль не найден")
        }
    val requiresEmailVerification: Boolean
        get() = status.equals("verify_email", ignoreCase = true)
}

data class DeleteAccountResult(
    val status: String,
    val message: String? = null
) {
    val isSuccess: Boolean
        get() = status.equals("success", ignoreCase = true)

    val requiresEmailCode: Boolean
        get() = status.equals("confirm_code", ignoreCase = true)
}

data class ChangePasswordResult(
    val status: String,
    val message: String? = null
) {
    val isSuccess: Boolean
        get() = status.equals("success", ignoreCase = true)

    val requiresEmailCode: Boolean
        get() = status.equals("confirm_code", ignoreCase = true)
}

data class ChangeEmailResult(
    val status: String,
    val message: String? = null
) {
    val isSuccess: Boolean
        get() = status.equals("success", ignoreCase = true)

    val requiresEmailCode: Boolean
        get() = status.equals("confirm_code", ignoreCase = true)
}

data class AuthGoldHistory(
    val status: Int = 0,
    val date: String? = null
)

data class AuthAccountChannel(
    val id: Int? = null,
    val name: String? = null,
    val username: String? = null,
    val avatar: PostImageAsset? = null
)

data class AuthSession(
    val id: String,
    val deviceType: Int? = null,
    val device: String? = null,
    val createDate: String? = null
)

data class AuthSessionsResult(
    val status: String,
    val message: String? = null,
    val currentSession: AuthSession? = null,
    val sessions: List<AuthSession> = emptyList()
) {
    val isSuccess: Boolean get() = status == "success"
}
