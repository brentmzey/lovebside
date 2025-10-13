package love.bside.app.data.storage

import com.russhwolf.settings.Settings

interface TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
    fun hasToken(): Boolean
}

class TokenStorageImpl(private val settings: Settings) : TokenStorage {
    override fun saveToken(token: String) {
        settings.putString(TOKEN_KEY, token)
    }

    override fun getToken(): String? {
        return settings.getStringOrNull(TOKEN_KEY)
    }

    override fun clearToken() {
        settings.remove(TOKEN_KEY)
    }

    override fun hasToken(): Boolean {
        return settings.hasKey(TOKEN_KEY)
    }

    companion object {
        private const val TOKEN_KEY = "auth_token"
    }
}
