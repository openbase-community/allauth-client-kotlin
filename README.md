# AllAuthClientKotlin

Kotlin/Android library scaffold for apps that authenticate against Django AllAuth headless APIs. It mirrors the core behavior of `allauth-client-swift`: dynamic JSON responses, auth state helpers, session-token fallback, memory-only access JWTs, secure refresh-token storage hooks, and one automatic JWT refresh retry on 401 responses.

The client expects a Django AllAuth headless base URL such as:

```text
https://example.com/_allauth/app/v1
```

## Setup

```kotlin
import community.openbase.allauth.client.AllAuthClient
import community.openbase.allauth.client.storage.AndroidAllAuthTokenStorage
import community.openbase.allauth.client.storage.SecureRefreshTokenStore

val refreshTokenStore = object : SecureRefreshTokenStore {
    override suspend fun readRefreshToken(): String? = null
    override suspend fun writeRefreshToken(token: String?) {
        // Store in Android Keystore, AndroidX Security, or an app-owned secure store.
    }
}

AllAuthClient.shared.setup(
    baseUrl = "https://example.com/_allauth/app/v1",
    tokenStorage = AndroidAllAuthTokenStorage(
        context = applicationContext,
        secureRefreshTokenStore = refreshTokenStore,
    ),
)
```

For tests or previews, use `InMemoryAllAuthTokenStorage`.

## Usage

```kotlin
val config = AllAuthClient.shared.getConfig()
val auth = AllAuthClient.shared.getAuth()

val login = AllAuthClient.shared.loginWithEmail(
    email = email,
    password = password,
)

val repository = AuthRepository(AllAuthClient.shared)
repository.initialize()
val authenticated = repository.state.value.isAuthenticated
```

## Implemented Surface

- URL definitions for config, auth, account, MFA, social-provider, token-refresh, and session endpoints.
- Auth endpoints: config, session auth, email login, username login, logout, signup, reauthenticate, login code request/confirm, password reset, and email verification.
- Account endpoints: email address management and password change.
- Session endpoints: list and delete sessions.
- Token storage abstraction with memory-only access token, persisted session token hooks, and secure refresh-token abstraction.
- Auth repository with `StateFlow` state comparable to the Swift `AuthContext`.

UI components are intentionally out of scope for this scaffold.

