package love.bside.app.data.storage

import com.russhwolf.settings.Settings

interface TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun saveRefreshToken(token: String)
    fun getRefreshToken(): String?
    fun clearTokens()
    fun hasToken(): Boolean
    
    // Legacy methods for backward compatibility
    fun clearToken() = clearTokens()
}

class TokenStorageImpl(private val settings: Settings) : TokenStorage {
    override fun saveToken(token: String) {
        settings.putString(TOKEN_KEY, token)
    }

    override fun getToken(): String? {
        return settings.getStringOrNull(TOKEN_KEY)
    }
    
    override fun saveRefreshToken(token: String) {
        settings.putString(REFRESH_TOKEN_KEY, token)
    }
    
    override fun getRefreshToken(): String? {
        return settings.getStringOrNull(REFRESH_TOKEN_KEY)
    }

    override fun clearTokens() {
        settings.remove(TOKEN_KEY)
        settings.remove(REFRESH_TOKEN_KEY)
    }

    override fun hasToken(): Boolean {
        return settings.hasKey(TOKEN_KEY)
    }

    companion object {
        private const val TOKEN_KEY = "auth_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }
}
