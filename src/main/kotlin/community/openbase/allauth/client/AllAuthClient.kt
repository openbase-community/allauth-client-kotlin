package community.openbase.allauth.client

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import community.openbase.allauth.client.storage.AllAuthTokenStorage
import community.openbase.allauth.client.storage.InMemoryAllAuthTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

public class AllAuthClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val moshi: Moshi = Moshi.Builder().build(),
    private val refreshScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    tokenStorage: AllAuthTokenStorage = InMemoryAllAuthTokenStorage(),
) {
    private val jsonMediaType = "application/json".toMediaType()
    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )
    private val refreshMutex = Mutex()
    private var inFlightRefresh: kotlinx.coroutines.Deferred<AllAuthResponse>? = null
    private var lastRefreshResponse: AllAuthResponse? = null
    private var tokenStorage: AllAuthTokenStorage = tokenStorage

    private val _authChanges = MutableSharedFlow<AuthChangeEvent>(replay = 1, extraBufferCapacity = 1)
    private val _lastAuthResponse = MutableStateFlow<AllAuthResponse?>(null)

    public var baseUrl: String = ""
        private set

    public var urls: AllAuthUrls? = null
        private set

    public var jwtAccessToken: String? = null

    public val authChanges: SharedFlow<AuthChangeEvent> = _authChanges
    public val lastAuthResponse: StateFlow<AllAuthResponse?> = _lastAuthResponse

    public fun setup(baseUrl: String, tokenStorage: AllAuthTokenStorage? = null) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.urls = AllAuthUrls(this.baseUrl)
        if (tokenStorage != null) {
            this.tokenStorage = tokenStorage
        }
    }

    public suspend fun getConfig(): AllAuthResponse =
        request("GET", requireUrls().config)

    public suspend fun getAuth(): AllAuthResponse =
        request("GET", requireUrls().session)

    public suspend fun login(
        identifier: String,
        password: String,
        identifierType: LoginIdentifier = LoginIdentifier.EMAIL,
    ): AllAuthResponse {
        val key = when (identifierType) {
            LoginIdentifier.EMAIL -> "email"
            LoginIdentifier.USERNAME -> "username"
        }
        return request("POST", requireUrls().login, mapOf(key to identifier, "password" to password))
    }

    public suspend fun loginWithEmail(email: String, password: String): AllAuthResponse =
        login(email, password, LoginIdentifier.EMAIL)

    public suspend fun loginWithUsername(username: String, password: String): AllAuthResponse =
        login(username, password, LoginIdentifier.USERNAME)

    public suspend fun logout(): AllAuthResponse {
        return try {
            val response = request("DELETE", requireUrls().session, autoRefreshJWT = false)
            expireSessionLocally()
            response
        } catch (_: Exception) {
            expireSessionLocally()
            AllAuthResponse(
                httpStatusCode = 200,
                body = mapOf(
                    "status" to 200,
                    "meta" to mapOf("is_authenticated" to false),
                    "data" to emptyMap<String, Any?>(),
                ),
            )
        }
    }

    public suspend fun signUp(
        email: String,
        password: String,
        username: String? = null,
    ): AllAuthResponse {
        val data = mutableMapOf<String, Any?>("email" to email, "password" to password)
        if (username != null) {
            data["username"] = username
        }
        return request("POST", requireUrls().signup, data)
    }

    public suspend fun reauthenticate(password: String): AllAuthResponse =
        request("POST", requireUrls().reauthenticate, mapOf("password" to password))

    public suspend fun requestLoginCode(email: String): AllAuthResponse =
        request("POST", requireUrls().requestLoginCode, mapOf("email" to email))

    public suspend fun confirmLoginCode(code: String): AllAuthResponse =
        request("POST", requireUrls().confirmLoginCode, mapOf("code" to code))

    public suspend fun requestPasswordReset(email: String): AllAuthResponse =
        request("POST", requireUrls().requestPasswordReset, mapOf("email" to email))

    public suspend fun getPasswordReset(key: String): AllAuthResponse =
        request("GET", requireUrls().resetPassword, headers = mapOf("X-Password-Reset-Key" to key))

    public suspend fun resetPassword(key: String, password: String): AllAuthResponse =
        request("POST", requireUrls().resetPassword, mapOf("key" to key, "password" to password))

    public suspend fun getEmailVerification(key: String): AllAuthResponse =
        request("GET", requireUrls().verifyEmail, headers = mapOf("X-Email-Verification-Key" to key))

    public suspend fun verifyEmail(key: String): AllAuthResponse =
        request("POST", requireUrls().verifyEmail, mapOf("key" to key))

    public suspend fun getEmailAddresses(): AllAuthResponse =
        request("GET", requireUrls().emailAddresses)

    public suspend fun addEmailAddress(email: String): AllAuthResponse =
        request("POST", requireUrls().emailAddresses, mapOf("email" to email))

    public suspend fun deleteEmailAddress(email: String): AllAuthResponse =
        request("DELETE", requireUrls().emailAddresses, mapOf("email" to email))

    public suspend fun setPrimaryEmailAddress(email: String): AllAuthResponse =
        request("PATCH", requireUrls().emailAddresses, mapOf("email" to email, "primary" to true))

    public suspend fun requestEmailVerification(email: String): AllAuthResponse =
        request("PUT", requireUrls().emailAddresses, mapOf("email" to email))

    public suspend fun changePassword(
        currentPassword: String?,
        newPassword: String,
    ): AllAuthResponse {
        val data = mutableMapOf<String, Any?>("new_password" to newPassword)
        if (currentPassword != null) {
            data["current_password"] = currentPassword
        }
        return request("POST", requireUrls().changePassword, data)
    }

    public suspend fun getSessions(): AllAuthResponse =
        request("GET", requireUrls().sessions)

    public suspend fun deleteSessions(ids: List<String>): AllAuthResponse =
        request("DELETE", requireUrls().sessions, mapOf("sessions" to ids))

    public suspend fun refreshJWT(): AllAuthResponse {
        val task = refreshMutex.withLock {
            inFlightRefresh ?: refreshScope.async { performJWTRefresh() }.also { inFlightRefresh = it }
        }

        return try {
            task.await().also { lastRefreshResponse = it }
        } finally {
            refreshMutex.withLock {
                if (inFlightRefresh == task) {
                    inFlightRefresh = null
                }
            }
        }
    }

    public suspend fun clearJWTTokens() {
        jwtAccessToken = null
        tokenStorage.writeRefreshToken(null)
    }

    public suspend fun expireSessionLocally() {
        tokenStorage.writeSessionToken(null)
        clearJWTTokens()
        val expired = AllAuthResponse(
            httpStatusCode = 401,
            body = mapOf(
                "status" to 401,
                "meta" to mapOf("is_authenticated" to false),
                "data" to emptyMap<String, Any?>(),
            ),
        )
        _lastAuthResponse.value = expired
        _authChanges.emit(AuthChangeEvent.LOGGED_OUT)
    }

    public suspend fun readSessionToken(): String? =
        tokenStorage.readSessionToken()

    public suspend fun writeSessionToken(token: String?) {
        tokenStorage.writeSessionToken(token)
    }

    public suspend fun readRefreshToken(): String? =
        tokenStorage.readRefreshToken()

    public suspend fun writeRefreshToken(token: String?) {
        tokenStorage.writeRefreshToken(token)
    }

    public suspend fun request(
        method: String,
        url: String,
        data: Map<String, Any?>? = null,
        headers: Map<String, String> = emptyMap(),
        autoRefreshJWT: Boolean = true,
    ): AllAuthResponse {
        val response = executeRequest(method, url, data, headers)
        val isTokenRefreshRequest = url == requireUrls().tokenRefresh

        if (isTokenRefreshRequest && response.httpStatusCode !in 200..299) {
            if (response.httpStatusCode in setOf(400, 401, 410)) {
                clearJWTTokens()
                throw AllAuthException.SessionExpired()
            }
            throw AllAuthException.ApiError("JWT refresh failed with status ${response.httpStatusCode}")
        }

        if (response.httpStatusCode == 401 && autoRefreshJWT && tokenStorage.readRefreshToken() != null) {
            refreshJWT()
            return request(method, url, data, headers, autoRefreshJWT = false)
        }

        storeTokens(response)

        if (response.httpStatusCode == 410) {
            tokenStorage.writeSessionToken(null)
            clearJWTTokens()
            throw AllAuthException.SessionExpired()
        }

        if (response.get("meta", "is_authenticated") != null) {
            handleAuthChange(response, _lastAuthResponse.value)
            _lastAuthResponse.value = response
        }

        return response
    }

    private suspend fun performJWTRefresh(): AllAuthResponse {
        val refreshToken = tokenStorage.readRefreshToken()
            ?: throw AllAuthException.ApiError("No refresh token available")
        val result = request(
            method = "POST",
            url = requireUrls().tokenRefresh,
            data = mapOf("refresh_token" to refreshToken),
            autoRefreshJWT = false,
        )
        val refreshedAccessToken = result.stringAt("data", "access_token")
            ?: result.stringAt("meta", "access_token")
        if (refreshedAccessToken.isNullOrEmpty()) {
            throw AllAuthException.InvalidResponse()
        }
        return result
    }

    private suspend fun executeRequest(
        method: String,
        url: String,
        data: Map<String, Any?>?,
        headers: Map<String, String>,
    ): AllAuthResponse = withContext(Dispatchers.IO) {
        val requestBody = if (data != null && method != "GET") {
            mapAdapter.toJson(data).toRequestBody(jsonMediaType)
        } else {
            null
        }

        val builder = try {
            Request.Builder().url(url)
        } catch (error: IllegalArgumentException) {
            throw AllAuthException.InvalidUrl(error)
        }

        builder.header("Accept", "application/json")
        builder.header("User-Agent", "django-allauth-kotlin-android")

        val accessToken = jwtAccessToken
        val sessionToken = tokenStorage.readSessionToken()
        when {
            accessToken != null -> builder.header("Authorization", "Bearer $accessToken")
            sessionToken != null -> builder.header("X-Session-Token", sessionToken)
        }

        for ((key, value) in headers) {
            builder.header(key, value)
        }

        if (requestBody != null) {
            builder.header("Content-Type", "application/json")
        }

        builder.method(method, requestBody)

        val okhttpResponse = try {
            httpClient.newCall(builder.build()).execute()
        } catch (error: IOException) {
            throw AllAuthException.ApiError(error.message ?: "Network request failed", error)
        }

        okhttpResponse.use { response ->
            val responseBody = response.body?.string().orEmpty()
            val decoded = if (responseBody.isBlank()) {
                emptyMap()
            } else {
                mapAdapter.fromJson(responseBody) ?: emptyMap()
            }
            AllAuthResponse(response.code, decoded)
        }
    }

    private suspend fun storeTokens(response: AllAuthResponse) {
        response.stringAt("meta", "session_token")?.let { tokenStorage.writeSessionToken(it) }

        val accessToken = response.stringAt("data", "access_token")
            ?: response.stringAt("meta", "access_token")
        if (accessToken != null) {
            jwtAccessToken = accessToken
        }

        val refreshToken = response.stringAt("data", "refresh_token")
            ?: response.stringAt("meta", "refresh_token")
        if (refreshToken != null) {
            tokenStorage.writeRefreshToken(refreshToken)
        }
    }

    private suspend fun handleAuthChange(response: AllAuthResponse, previousAuth: AllAuthResponse?) {
        val previouslyAuthenticated = previousAuth?.isAuthenticated ?: false
        val currentlyAuthenticated = response.isAuthenticated
        val event = when {
            !previouslyAuthenticated && currentlyAuthenticated && response.status == 200 ->
                AuthChangeEvent.LOGGED_IN

            previouslyAuthenticated && !currentlyAuthenticated && response.status == 401 ->
                AuthChangeEvent.LOGGED_OUT

            response.status == 401 && response.flows.any {
                it["id"] == AuthFlow.REAUTHENTICATE.id || it["id"] == AuthFlow.MFA_REAUTHENTICATE.id
            } -> AuthChangeEvent.REAUTHENTICATION_REQUIRED

            response.status == 200 && response.get("data", "flows") != null ->
                AuthChangeEvent.FLOW_UPDATED

            else -> null
        }

        if (event != null) {
            _authChanges.emit(event)
        }
    }

    private fun requireUrls(): AllAuthUrls =
        urls ?: throw IllegalStateException("Call AllAuthClient.setup(baseUrl) before using the client.")

    public companion object {
        public val shared: AllAuthClient = AllAuthClient()
    }
}
