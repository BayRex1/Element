package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.UserApiRemoteDataSource
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.AuthAccountChannel
import elemsocial.com.domain.model.AuthGoldHistory
import elemsocial.com.domain.model.AuthResult
import elemsocial.com.domain.model.AuthSession
import elemsocial.com.domain.model.AuthSessionsResult
import elemsocial.com.domain.model.DeleteAccountResult
import elemsocial.com.domain.model.ChangePasswordResult
import elemsocial.com.domain.model.ChangeEmailResult
import elemsocial.com.domain.repository.AuthRepository

class AuthRepositoryImpl(
    socketClient: ElementSocketClient
) : AuthRepository {
    private val remote = UserApiRemoteDataSource(socketClient)

    override suspend fun login(email: String, password: String): AuthResult {
        return parseAuthResult(remote.login(email, password))
    }

    override suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        referralCode: String?,
        acceptRules: Boolean,
        hCaptchaToken: String
    ): AuthResult {
        return parseAuthResult(
            remote.register(
                name = name,
                username = username,
                email = email,
                password = password,
                referralCode = referralCode,
                acceptRules = acceptRules,
                hCaptchaToken = hCaptchaToken
            )
        )
    }

    override suspend fun verifyEmail(email: String, code: String): AuthResult {
        return parseAuthResult(remote.verifyEmail(email, code))
    }

    override suspend fun resendVerification(email: String): AuthResult {
        return parseAuthResult(remote.resendVerification(email))
    }

    override suspend fun connectBySessionKey(sessionKey: String): AuthResult {
        return parseAuthResult(remote.authorizationConnect(sessionKey))
    }

    override suspend fun logout(sessionKey: String): AuthResult {
        return parseAuthResult(remote.authorizationLogout(sessionKey))
    }

    override suspend fun loadSessions(): AuthSessionsResult {
        val response = remote.loadSessions()
        val status = response["status"]?.toString()
            ?: if (response.containsKey("sessions")) "success" else "error"
        val currentSession = parseAuthSession(response["current_session"])
        val sessions = (response["sessions"] as? List<*>)
            .orEmpty()
            .mapNotNull(::parseAuthSession)

        return AuthSessionsResult(
            status = status,
            message = response["message"]?.toString(),
            currentSession = currentSession,
            sessions = sessions
        )
    }

    override suspend fun deleteSession(sessionId: String): ActionResult {
        val response = remote.deleteSession(sessionId)
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

    override suspend fun requestDeleteAccount(
        password: String,
        deletePosts: Boolean
    ): DeleteAccountResult {
        return parseDeleteAccountResult(
            remote.requestDeleteAccount(
                password = password,
                deletePosts = deletePosts
            )
        )
    }

    override suspend fun confirmDeleteAccount(
        code: String,
        deletePosts: Boolean
    ): DeleteAccountResult {
        return parseDeleteAccountResult(
            remote.confirmDeleteAccount(
                code = code,
                deletePosts = deletePosts
            )
        )
    }

    override suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        code: String?
    ): ChangePasswordResult {
        return parseChangePasswordResult(
            remote.changePassword(
                oldPassword = oldPassword,
                newPassword = newPassword,
                code = code
            )
        )
    }

    override suspend fun changeUsername(username: String): ActionResult {
        return parseActionResult(remote.changeUsername(username))
    }

    override suspend fun changeEmail(
        email: String,
        code: String?
    ): ChangeEmailResult {
        return parseChangeEmailResult(
            remote.changeEmail(
                email = email,
                code = code
            )
        )
    }

    private fun parseAuthResult(response: Map<String, Any?>): AuthResult {
        val status = normalizeAuthStatus(
            rawStatus = response["status"]?.toString(),
            response = response
        )
        val message = response["message"]?.toString()
        val method = response["method"]?.toString()
        val sKey = response["S_KEY"]?.toString()
        val data = response["data"].asRichMap()

        val accountIdTop = response["accountID"].asInt()
            ?: response["account_id"].asInt()
            ?: data?.get("accountID").asInt()
            ?: data?.get("account_id").asInt()

        val accountData = response["accountData"].asRichMap()
            ?: data?.get("accountData").asRichMap()
            ?: data?.get("account").asRichMap()
            ?: data?.takeIf { it.containsKey("id") || it.containsKey("username") }

        val email = response["email"]?.toString()?.takeIf { it.isNotBlank() }
            ?: accountData?.get("email")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("email")?.toString()?.takeIf { it.isNotBlank() }

        val accountIdFromData = accountData?.get("id").asInt()
        val accountName = accountData?.get("name")?.toString()?.takeIf { it.isNotBlank() }
            ?: data?.get("name")?.toString()?.takeIf { it.isNotBlank() }
        val accountUsername = sanitizeUsername(
            accountData?.get("username")?.toString()
                ?: data?.get("username")?.toString()
                ?: response["accountUsername"]?.toString()
                ?: response["username"]?.toString()
        )
        val accountAvatar = parseAsset(accountData?.get("avatar"))
        val accountBalance = accountData?.get("e_balls").asDouble()
        val goldStatus = accountData?.get("gold_status").asBoolean()
        val goldHistory = parseGoldHistory(accountData?.get("gold_history"))
        val accountChannels = parseAccountChannels(accountData?.get("channels"))
        val notificationsCount = accountData?.get("notifications").asInt(0) ?: 0
        val permissions = accountData?.get("permissions").asRichMap()
            ?: data?.get("permissions").asRichMap()
        val isAdmin = permissions?.get("Admin").asBoolean() || permissions?.get("admin").asBoolean()

        return AuthResult(
            status = status,
            message = message,
            method = method,
            email = email,
            sessionKey = sKey,
            accountId = accountIdFromData ?: accountIdTop,
            accountName = accountName,
            accountUsername = accountUsername,
            accountAvatar = accountAvatar,
            accountBalance = accountBalance,
            goldStatus = goldStatus,
            goldHistory = goldHistory,
            accountChannels = accountChannels,
            notificationsCount = notificationsCount,
            isAdmin = isAdmin,
            raw = response
        )
    }

    private fun parseDeleteAccountResult(response: Map<String, Any?>): DeleteAccountResult {
        val status = response["status"]?.toString()?.trim()?.lowercase()
        val normalized = when (status) {
            "success", "ok", "200" -> "success"
            "confirm_code" -> "confirm_code"
            else -> "error"
        }

        return DeleteAccountResult(
            status = normalized,
            message = response["message"]?.toString()
        )
    }

    private fun parseChangePasswordResult(response: Map<String, Any?>): ChangePasswordResult {
        val status = response["status"]?.toString()?.trim()?.lowercase()
        val normalized = when (status) {
            "success", "ok", "200" -> "success"
            "confirm_code" -> "confirm_code"
            else -> "error"
        }

        return ChangePasswordResult(
            status = normalized,
            message = response["message"]?.toString()
        )
    }

    private fun parseChangeEmailResult(response: Map<String, Any?>): ChangeEmailResult {
        val status = response["status"]?.toString()?.trim()?.lowercase()
        val normalized = when (status) {
            "success", "ok", "200" -> "success"
            "confirm_code" -> "confirm_code"
            else -> "error"
        }

        return ChangeEmailResult(
            status = normalized,
            message = response["message"]?.toString()
        )
    }
}

private fun parseActionResult(response: Map<String, Any?>): ActionResult {
    val status = response["status"]?.toString()?.trim()?.lowercase()
    val normalized = when (status) {
        "success", "ok", "200", null, "" -> "success"
        else -> "error"
    }

    return ActionResult(
        status = normalized,
        message = response["message"]?.toString()
    )
}

private fun normalizeAuthStatus(
    rawStatus: String?,
    response: Map<String, Any?>
): String {
    val normalized = rawStatus?.trim()?.lowercase()
    return when (normalized) {
        "success", "ok", "200" -> "success"
        "verify_email" -> "verify_email"
        null, "" -> {
            val hasAuthPayload = response["S_KEY"]?.toString()?.isNotBlank() == true ||
                response["accountData"] != null ||
                response["accountID"] != null ||
                response["account_id"] != null
            if (hasAuthPayload) "success" else "unknown"
        }

        else -> normalized
    }
}

private fun sanitizeUsername(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    return trimmed
        .removePrefix("@")
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun parseGoldHistory(raw: Any?): List<AuthGoldHistory> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        AuthGoldHistory(
            status = (map["status"] as? Number)?.toInt()
                ?: map["status"]?.toString()?.toIntOrNull()
                ?: 0,
            date = map["date"]?.toString()
        )
    }
}

private fun parseAuthSession(raw: Any?): AuthSession? {
    val map = raw as? Map<*, *> ?: return null
    val id = map["id"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    return AuthSession(
        id = id,
        deviceType = (map["device_type"] as? Number)?.toInt()
            ?: map["device_type"]?.toString()?.toIntOrNull(),
        device = map["device"]?.toString(),
        createDate = map["create_date"]?.toString()
    )
}

private fun parseAccountChannels(raw: Any?): List<AuthAccountChannel> {
    val source = raw as? List<*> ?: return emptyList()
    return source.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        AuthAccountChannel(
            id = (map["id"] as? Number)?.toInt(),
            name = map["name"]?.toString(),
            username = map["username"]?.toString(),
            avatar = parseAsset(map["avatar"])
        )
    }
}
