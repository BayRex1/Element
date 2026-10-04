package elemsocial.com.domain.repository

import elemsocial.com.domain.model.AuthResult
import elemsocial.com.domain.model.AuthSessionsResult
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.DeleteAccountResult
import elemsocial.com.domain.model.ChangePasswordResult
import elemsocial.com.domain.model.ChangeEmailResult

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        referralCode: String?,
        acceptRules: Boolean,
        hCaptchaToken: String
    ): AuthResult

    suspend fun verifyEmail(email: String, code: String): AuthResult
    suspend fun resendVerification(email: String): AuthResult
    suspend fun connectBySessionKey(sessionKey: String): AuthResult
    suspend fun logout(sessionKey: String): AuthResult
    suspend fun loadSessions(): AuthSessionsResult
    suspend fun deleteSession(sessionId: String): ActionResult
    suspend fun requestDeleteAccount(password: String, deletePosts: Boolean): DeleteAccountResult
    suspend fun confirmDeleteAccount(code: String, deletePosts: Boolean): DeleteAccountResult
    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        code: String? = null
    ): ChangePasswordResult
    suspend fun changeUsername(username: String): ActionResult
    suspend fun changeEmail(
        email: String,
        code: String? = null
    ): ChangeEmailResult
}
