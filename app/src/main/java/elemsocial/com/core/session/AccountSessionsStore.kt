package elemsocial.com.core.session

import android.content.Context
import elemsocial.com.domain.model.AuthResult
import elemsocial.com.domain.model.PostImageAsset
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "element_accounts"
private const val KEY_ACTIVE_SESSION = "active_session"
private const val KEY_ACCOUNTS = "accounts"

data class SavedAccountSession(
    val sessionKey: String,
    val accountId: Int? = null,
    val name: String? = null,
    val username: String? = null,
    val avatar: PostImageAsset? = null,
    val balance: Double? = null,
    val isAdmin: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

class AccountSessionsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getActiveSessionKey(): String? {
        return prefs.getString(KEY_ACTIVE_SESSION, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun setActiveSessionKey(sessionKey: String?) {
        val editor = prefs.edit()
        if (sessionKey.isNullOrBlank()) {
            editor.remove(KEY_ACTIVE_SESSION)
        } else {
            editor.putString(KEY_ACTIVE_SESSION, sessionKey)
        }
        editor.apply()
    }

    fun getAccounts(): List<SavedAccountSession> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (index in 0 until arr.length()) {
                    val obj = arr.optJSONObject(index) ?: continue
                    val sessionKey = obj.optString("sessionKey").trim()
                    if (sessionKey.isBlank()) continue
                    add(
                        SavedAccountSession(
                            sessionKey = sessionKey,
                            accountId = obj.optInt("accountId").takeIf { obj.has("accountId") },
                            name = obj.optString("name").takeIf { it.isNotBlank() },
                            username = normalizeSessionUsername(obj.optString("username")),
                            avatar = obj.optJSONObject("avatar")?.toAsset(),
                            balance = obj.optDouble("balance").takeIf { obj.has("balance") },
                            isAdmin = obj.optBoolean("isAdmin", false),
                            updatedAt = obj.optLong("updatedAt").takeIf { it > 0L } ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
            .dedupeSavedAccounts()
    }

    fun upsertFromAuth(sessionKey: String, auth: AuthResult) {
        val normalized = sessionKey.trim()
        if (normalized.isBlank()) return

        val current = getAccounts().toMutableList()
        val existing = current.firstOrNull { it.sessionKey == normalized }

        val merged = SavedAccountSession(
            sessionKey = normalized,
            accountId = auth.accountId ?: existing?.accountId,
            name = auth.accountName ?: existing?.name,
            username = normalizeSessionUsername(auth.accountUsername)
                ?: normalizeSessionUsername(existing?.username),
            avatar = auth.accountAvatar ?: existing?.avatar,
            balance = auth.accountBalance ?: existing?.balance,
            isAdmin = auth.isAdmin,
            updatedAt = System.currentTimeMillis()
        )

        val next = current
            .filterNot {
                it.isSameAccount(
                    sessionKey = normalized,
                    accountId = merged.accountId,
                    username = merged.username
                )
            }
            .plus(merged)
            .sortedByDescending { it.updatedAt }
            .dedupeSavedAccounts()

        saveAccounts(next)
        setActiveSessionKey(normalized)
    }

    fun removeSession(sessionKey: String) {
        val normalized = sessionKey.trim()
        if (normalized.isBlank()) return

        val next = getAccounts().filterNot { it.sessionKey == normalized }
        saveAccounts(next)

        if (getActiveSessionKey() == normalized) {
            setActiveSessionKey(next.firstOrNull()?.sessionKey)
        }
    }

    fun clearAll() {
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION)
            .remove(KEY_ACCOUNTS)
            .apply()
    }

    private fun saveAccounts(accounts: List<SavedAccountSession>) {
        val arr = JSONArray()
        accounts.forEach { account ->
            val obj = JSONObject()
                .put("sessionKey", account.sessionKey)
                .put("updatedAt", account.updatedAt)
                .put("isAdmin", account.isAdmin)

            account.accountId?.let { obj.put("accountId", it) }
            account.name?.let { obj.put("name", it) }
            account.username?.let { obj.put("username", it) }
            account.balance?.let { obj.put("balance", it) }
            account.avatar?.let { avatar ->
                obj.put("avatar", JSONObject().apply {
                    put("path", avatar.path)
                    put("file", avatar.file)
                    avatar.simple?.let { put("simple", it) }
                    avatar.aura?.let { put("aura", it) }
                    avatar.preview?.let { put("preview", it) }
                })
            }

            arr.put(obj)
        }

        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }
}

private fun List<SavedAccountSession>.dedupeSavedAccounts(): List<SavedAccountSession> {
    val seenSessionKeys = mutableSetOf<String>()
    val seenIdentityKeys = mutableSetOf<String>()

    return buildList {
        for (account in this@dedupeSavedAccounts) {
            if (!seenSessionKeys.add(account.sessionKey)) continue

            val identityKey = account.identityKey()
            if (identityKey != null && !seenIdentityKeys.add(identityKey)) {
                continue
            }

            add(account)
        }
    }
}

private fun SavedAccountSession.isSameAccount(
    sessionKey: String? = null,
    accountId: Int? = null,
    username: String? = null
): Boolean {
    if (!sessionKey.isNullOrBlank() && this.sessionKey == sessionKey.trim()) {
        return true
    }

    val ownIdentity = identityKey()
    val otherIdentity = buildSavedAccountIdentity(accountId = accountId, username = username)
    return ownIdentity != null && otherIdentity != null && ownIdentity == otherIdentity
}

private fun SavedAccountSession.identityKey(): String? {
    return buildSavedAccountIdentity(accountId = accountId, username = username)
}

private fun buildSavedAccountIdentity(
    accountId: Int?,
    username: String?
): String? {
    return accountId?.let { "id:$it" }
        ?: normalizeSessionUsername(username)?.let { "username:$it" }
}

private fun JSONObject.toAsset(): PostImageAsset? {
    val path = optString("path").takeIf { it.isNotBlank() } ?: return null
    val file = optString("file").takeIf { it.isNotBlank() } ?: return null
    return PostImageAsset(
        path = path,
        file = file,
        simple = optString("simple").takeIf { it.isNotBlank() },
        aura = optString("aura").takeIf { it.isNotBlank() },
        preview = optString("preview").takeIf { it.isNotBlank() }
    )
}

private fun normalizeSessionUsername(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    return trimmed
        .removePrefix("@")
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeIf { it.isNotBlank() }
}
