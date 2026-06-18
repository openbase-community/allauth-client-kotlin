package community.openbase.allauth.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class AuthState(
    val auth: AllAuthResponse? = null,
    val config: AllAuthResponse? = null,
    val isLoading: Boolean = true,
    val lastAuthChange: AuthChangeEvent? = null,
) {
    public val isAuthenticated: Boolean get() = auth?.isAuthenticated ?: false
    public val requiresReauthentication: Boolean
        get() {
            val currentAuth = auth ?: return false
            return currentAuth.status == 401 && currentAuth.flows.any {
                it["id"] == AuthFlow.REAUTHENTICATE.id || it["id"] == AuthFlow.MFA_REAUTHENTICATE.id
            }
        }
    public val user: Map<String, Any?>? get() = auth?.user
    public val pendingFlows: List<Map<String, Any?>> get() = auth?.pendingFlows ?: emptyList()
    public val emailAuthEnabled: Boolean get() = config?.stringAt("data", "account", "authentication_method") != "username"
    public val usernameAuthEnabled: Boolean
        get() = config?.stringAt("data", "account", "authentication_method").let {
            it == "username" || it == "username_email"
        }
    public val signupAllowed: Boolean get() = config?.booleanAt("data", "account", "is_open_for_signup") ?: true
    public val loginByCodeEnabled: Boolean get() = config?.booleanAt("data", "account", "login_by_code_enabled") ?: false
    public val mfaEnabled: Boolean get() = config?.booleanAt("data", "mfa", "enabled") ?: false
    public val socialProviders: List<Map<String, Any?>>
        get() = config?.listAt("data", "socialaccount", "providers")?.mapNotNull { it as? Map<String, Any?> }
            ?: emptyList()

    public fun isPending(flow: AuthFlow): Boolean =
        pendingFlows.any { it["id"] == flow.id }

    public fun getPendingFlow(flow: AuthFlow): Map<String, Any?>? =
        pendingFlows.firstOrNull { it["id"] == flow.id }

    public fun provider(id: String): Map<String, Any?>? =
        socialProviders.firstOrNull { it["id"] == id }
}

public class AuthRepository(
    private val client: AllAuthClient = AllAuthClient.shared,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(AuthState())

    public val state: StateFlow<AuthState> = _state

    init {
        scope.launch {
            client.authChanges.collect { event ->
                _state.update { it.copy(lastAuthChange = event) }
            }
        }
        scope.launch {
            client.lastAuthResponse.collect { response ->
                if (response != null && response.status in setOf(200, 401)) {
                    _state.update { it.copy(auth = response) }
                }
            }
        }
    }

    public suspend fun initialize() {
        _state.update { it.copy(isLoading = true) }
        try {
            val config = client.getConfig()
            val auth = client.getAuth()
            _state.update { it.copy(config = config, auth = auth, isLoading = false) }
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false) }
        }
    }

    public suspend fun refreshAuth() {
        runCatching { client.getAuth() }
            .onSuccess { auth -> _state.update { it.copy(auth = auth) } }
    }

    public suspend fun refreshConfig() {
        runCatching { client.getConfig() }
            .onSuccess { config -> _state.update { it.copy(config = config) } }
    }

    public fun clearAuth() {
        _state.update { it.copy(auth = null) }
    }

    public fun detectAuthChange(
        previousAuth: AllAuthResponse?,
        currentAuth: AllAuthResponse?,
    ): AuthChangeEvent? {
        val wasAuthenticated = previousAuth?.isAuthenticated ?: false
        val isNowAuthenticated = currentAuth?.isAuthenticated ?: false
        val currentStatus = currentAuth?.status ?: 0

        return when {
            !wasAuthenticated && isNowAuthenticated && currentStatus == 200 -> AuthChangeEvent.LOGGED_IN
            wasAuthenticated && !isNowAuthenticated && currentStatus == 401 -> AuthChangeEvent.LOGGED_OUT
            currentStatus == 401 && currentAuth?.flows?.any {
                it["id"] == AuthFlow.REAUTHENTICATE.id || it["id"] == AuthFlow.MFA_REAUTHENTICATE.id
            } == true -> AuthChangeEvent.REAUTHENTICATION_REQUIRED
            currentStatus == 200 && currentAuth?.get("data", "flows") != null -> AuthChangeEvent.FLOW_UPDATED
            else -> null
        }
    }
}
