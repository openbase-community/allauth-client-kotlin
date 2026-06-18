package community.openbase.allauth.client.storage

import android.content.Context

public interface SecureRefreshTokenStore {
    public suspend fun readRefreshToken(): String?
    public suspend fun writeRefreshToken(token: String?)
}

public interface AllAuthTokenStorage : SecureRefreshTokenStore {
    public suspend fun readSessionToken(): String?
    public suspend fun writeSessionToken(token: String?)
}

public class InMemoryAllAuthTokenStorage(
    initialSessionToken: String? = null,
    initialRefreshToken: String? = null,
) : AllAuthTokenStorage {
    private var sessionToken: String? = initialSessionToken
    private var refreshToken: String? = initialRefreshToken

    override suspend fun readSessionToken(): String? = sessionToken

    override suspend fun writeSessionToken(token: String?) {
        sessionToken = token
    }

    override suspend fun readRefreshToken(): String? = refreshToken

    override suspend fun writeRefreshToken(token: String?) {
        refreshToken = token
    }
}

public class AndroidAllAuthTokenStorage(
    context: Context,
    private val secureRefreshTokenStore: SecureRefreshTokenStore,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
    private val sessionTokenKey: String = DEFAULT_SESSION_TOKEN_KEY,
) : AllAuthTokenStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE,
    )

    override suspend fun readSessionToken(): String? =
        preferences.getString(sessionTokenKey, null)

    override suspend fun writeSessionToken(token: String?) {
        preferences.edit().apply {
            if (token == null) {
                remove(sessionTokenKey)
            } else {
                putString(sessionTokenKey, token)
            }
        }.apply()
    }

    override suspend fun readRefreshToken(): String? =
        secureRefreshTokenStore.readRefreshToken()

    override suspend fun writeRefreshToken(token: String?) {
        secureRefreshTokenStore.writeRefreshToken(token)
    }

    public companion object {
        public const val DEFAULT_PREFERENCES_NAME: String = "allauth_client"
        public const val DEFAULT_SESSION_TOKEN_KEY: String = "allauth_session_token"
    }
}

