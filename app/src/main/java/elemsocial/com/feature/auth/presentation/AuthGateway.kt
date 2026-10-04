package elemsocial.com.feature.auth.presentation

import elemsocial.com.domain.model.AuthResult
import elemsocial.com.domain.model.AuthSessionsResult
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.DeleteAccountResult
import elemsocial.com.domain.model.ChangePasswordResult
import elemsocial.com.domain.model.ChangeEmailResult
import elemsocial.com.domain.repository.AuthRepository

class AuthGateway(
    private val authRepository: AuthRepository
) {
    suspend fun login(email: String, password: String): AuthResult {
        return authRepository.login(email, password)
    }

    suspend fun register(
        name: String,
        username: String,
        email: String,
        password: String,
        referralCode: String?,
        acceptRules: Boolean,
        hCaptchaToken: String
    ): AuthResult {
        return authRepository.register(
            name = name,
            username = username,
            email = email,
            password = password,
            referralCode = referralCode,
            acceptRules = acceptRules,
            hCaptchaToken = hCaptchaToken
        )
    }

    suspend fun verifyEmail(email: String, code: String): AuthResult {
        return authRepository.verifyEmail(email, code)
    }

    suspend fun resendVerification(email: String): AuthResult {
        return authRepository.resendVerification(email)
    }

    suspend fun connectBySessionKey(sessionKey: String): AuthResult {
        return authRepository.connectBySessionKey(sessionKey)
    }

    suspend fun logout(sessionKey: String): AuthResult {
        return authRepository.logout(sessionKey)
    }

    suspend fun loadSessions(): AuthSessionsResult {
        return authRepository.loadSessions()
    }

    suspend fun deleteSession(sessionId: String): ActionResult {
        return authRepository.deleteSession(sessionId)
    }

    suspend fun requestDeleteAccount(password: String, deletePosts: Boolean): DeleteAccountResult {
        return authRepository.requestDeleteAccount(
            password = password,
            deletePosts = deletePosts
        )
    }

    suspend fun confirmDeleteAccount(code: String, deletePosts: Boolean): DeleteAccountResult {
        return authRepository.confirmDeleteAccount(
            code = code,
            deletePosts = deletePosts
        )
    }

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        code: String? = null
    ): ChangePasswordResult {
        return authRepository.changePassword(
            oldPassword = oldPassword,
            newPassword = newPassword,
            code = code
        )
    }

    suspend fun changeUsername(username: String): ActionResult {
        return authRepository.changeUsername(username)
    }

    suspend fun changeEmail(
        email: String,
        code: String? = null
    ): ChangeEmailResult {
        return authRepository.changeEmail(
            email = email,
            code = code
        )
    }
}
