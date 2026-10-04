package elemsocial.com.data.remote

import elemsocial.com.config.AppConfig
import elemsocial.com.core.ws.ElementSocketClient

class UserApiRemoteDataSource(
    private val socketClient: ElementSocketClient
) {
    suspend fun authorizationConnect(sessionKey: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "authorization",
                "action" to "connect",
                "S_KEY" to sessionKey
            )
        )
    }

    suspend fun authorizationLogout(sessionKey: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "authorization",
                "action" to "logout",
                "S_KEY" to sessionKey
            )
        )
    }

    suspend fun login(email: String, password: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "auth/login",
                "email" to email,
                "password" to password,
                "device_type" to AppConfig.Session.DEVICE_TYPE,
                "device" to AppConfig.Session.DEVICE_NAME,
            )
        )
    }

    suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        referralCode: String?,
        acceptRules: Boolean,
        hCaptchaToken: String
    ): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>(
            "type" to "social",
            "action" to "auth/reg",
            "name" to name,
            "username" to username,
            "email" to email,
            "password" to password,
            "referral_code" to referralCode,
            "accept" to acceptRules,
            "h_captcha" to hCaptchaToken,
        )

        return socketClient.sendRequest(payload)
    }

    suspend fun verifyEmail(email: String, code: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "auth/verify_email",
                "email" to email,
                "code" to code,
                "device_type" to AppConfig.Session.DEVICE_TYPE,
                "device" to AppConfig.Session.DEVICE_NAME,
            )
        )
    }

    suspend fun resendVerification(email: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "auth/resend_verification",
                "email" to email,
            )
        )
    }

    suspend fun loadSessions(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "auth/sessions/load"
            )
        )
    }

    suspend fun deleteSession(sessionId: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "auth/sessions/delete",
                "session_id" to sessionId
            )
        )
    }

    suspend fun requestDeleteAccount(
        password: String,
        deletePosts: Boolean
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "account/delete",
                "payload" to mapOf(
                    "password" to password,
                    "delete_posts" to deletePosts
                )
            )
        )
    }

    suspend fun confirmDeleteAccount(
        code: String,
        deletePosts: Boolean
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "account/delete_confirm",
                "payload" to mapOf(
                    "code" to code,
                    "delete_posts" to deletePosts
                )
            )
        )
    }

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        code: String? = null
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            buildMap {
                put("type", "social")
                put("action", "change_profile/password")
                put("old_password", oldPassword)
                put("new_password", newPassword)
                code?.takeIf { it.isNotBlank() }?.let { put("code", it) }
            }
        )
    }

    suspend fun changeUsername(username: String): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "change_profile/username",
                "username" to username
            )
        )
    }

    suspend fun changeEmail(
        email: String,
        code: String? = null
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            buildMap {
                put("type", "social")
                put("action", "change_profile/email")
                put("email", email)
                code?.takeIf { it.isNotBlank() }?.let { put("code", it) }
            }
        )
    }
}
