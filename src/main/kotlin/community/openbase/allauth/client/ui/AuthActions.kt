package community.openbase.allauth.client.ui

import community.openbase.allauth.client.AllAuthClient
import community.openbase.allauth.client.AllAuthResponse
import community.openbase.allauth.client.AuthRepository
import community.openbase.allauth.client.AuthState
import community.openbase.allauth.client.resolveLoginIdentifier
import kotlinx.coroutines.flow.StateFlow

/**
 * The auth surface the reusable Compose UI depends on.
 *
 * This is deliberately narrow: it exposes the observable [state] plus the
 * account/auth operations the bundled screens invoke. Consuming apps can either
 * use [AllAuthActions] (backed by [AllAuthClient] + [AuthRepository]) or supply
 * their own implementation when they need product-specific wiring (for example
 * Openbase's JWT reissue) while still reusing the UI.
 */
public interface AuthActions {
    public val state: StateFlow<AuthState>

    public suspend fun initialize()

    public suspend fun login(identifier: String, password: String)

    public suspend fun signUp(email: String, password: String, username: String?): AllAuthResponse

    public suspend fun requestLoginCode(email: String): AllAuthResponse

    public suspend fun confirmLoginCode(code: String): AllAuthResponse

    public suspend fun requestPasswordReset(email: String): AllAuthResponse

    public suspend fun resetPassword(key: String, password: String): AllAuthResponse

    public suspend fun getEmailVerification(key: String): AllAuthResponse

    public suspend fun verifyEmail(key: String): AllAuthResponse

    public suspend fun authenticateTOTP(code: String): AllAuthResponse

    public suspend fun authenticateWithRecoveryCode(code: String): AllAuthResponse

    public suspend fun getEmailAddresses(): AllAuthResponse

    public suspend fun addEmailAddress(email: String): AllAuthResponse

    public suspend fun deleteEmailAddress(email: String): AllAuthResponse

    public suspend fun setPrimaryEmailAddress(email: String): AllAuthResponse

    public suspend fun requestEmailVerification(email: String): AllAuthResponse

    public suspend fun changePassword(currentPassword: String?, newPassword: String): AllAuthResponse

    public suspend fun reauthenticate(password: String): AllAuthResponse

    public suspend fun getSessions(): AllAuthResponse

    public suspend fun deleteSessions(ids: List<String>): AllAuthResponse

    public suspend fun getAuthenticators(): AllAuthResponse

    public suspend fun getTOTPAuthenticator(): AllAuthResponse

    public suspend fun activateTOTP(code: String): AllAuthResponse

    public suspend fun deactivateTOTP(): AllAuthResponse

    public suspend fun getRecoveryCodes(): AllAuthResponse

    public suspend fun generateRecoveryCodes(): AllAuthResponse

    public suspend fun getProviders(): AllAuthResponse

    public suspend fun disconnectProvider(providerId: String, accountUid: String): AllAuthResponse
}

/**
 * Default [AuthActions] implementation backed by an [AllAuthClient] and an
 * [AuthRepository]. Call [AllAuthClient.setup] before [initialize].
 */
public open class AllAuthActions(
    private val client: AllAuthClient,
    private val repository: AuthRepository = AuthRepository(client),
) : AuthActions {
    override val state: StateFlow<AuthState> get() = repository.state

    override suspend fun initialize() {
        repository.initialize()
    }

    override suspend fun login(identifier: String, password: String) {
        val authState = repository.state.value
        val identifierType = resolveLoginIdentifier(
            emailEnabled = authState.emailAuthEnabled,
            usernameEnabled = authState.usernameAuthEnabled,
            identifier = identifier,
        )
        client.login(identifier = identifier, password = password, identifierType = identifierType)
    }

    override suspend fun signUp(email: String, password: String, username: String?): AllAuthResponse =
        client.signUp(email = email, password = password, username = username?.takeIf { it.isNotBlank() })

    override suspend fun requestLoginCode(email: String): AllAuthResponse =
        client.requestLoginCode(email)

    override suspend fun confirmLoginCode(code: String): AllAuthResponse =
        client.confirmLoginCode(code)

    override suspend fun requestPasswordReset(email: String): AllAuthResponse =
        client.requestPasswordReset(email)

    override suspend fun resetPassword(key: String, password: String): AllAuthResponse =
        client.resetPassword(key, password)

    override suspend fun getEmailVerification(key: String): AllAuthResponse =
        client.getEmailVerification(key)

    override suspend fun verifyEmail(key: String): AllAuthResponse =
        client.verifyEmail(key)

    override suspend fun authenticateTOTP(code: String): AllAuthResponse =
        client.authenticateTOTP(code)

    override suspend fun authenticateWithRecoveryCode(code: String): AllAuthResponse =
        client.authenticateWithRecoveryCode(code)

    override suspend fun getEmailAddresses(): AllAuthResponse =
        client.getEmailAddresses()

    override suspend fun addEmailAddress(email: String): AllAuthResponse =
        client.addEmailAddress(email)

    override suspend fun deleteEmailAddress(email: String): AllAuthResponse =
        client.deleteEmailAddress(email)

    override suspend fun setPrimaryEmailAddress(email: String): AllAuthResponse =
        client.setPrimaryEmailAddress(email)

    override suspend fun requestEmailVerification(email: String): AllAuthResponse =
        client.requestEmailVerification(email)

    override suspend fun changePassword(currentPassword: String?, newPassword: String): AllAuthResponse =
        client.changePassword(currentPassword?.takeIf { it.isNotBlank() }, newPassword)

    override suspend fun reauthenticate(password: String): AllAuthResponse =
        client.reauthenticate(password)

    override suspend fun getSessions(): AllAuthResponse =
        client.getSessions()

    override suspend fun deleteSessions(ids: List<String>): AllAuthResponse =
        client.deleteSessions(ids)

    override suspend fun getAuthenticators(): AllAuthResponse =
        client.getAuthenticators()

    override suspend fun getTOTPAuthenticator(): AllAuthResponse =
        client.getTOTPAuthenticator()

    override suspend fun activateTOTP(code: String): AllAuthResponse =
        client.activateTOTP(code)

    override suspend fun deactivateTOTP(): AllAuthResponse =
        client.deactivateTOTP()

    override suspend fun getRecoveryCodes(): AllAuthResponse =
        client.getRecoveryCodes()

    override suspend fun generateRecoveryCodes(): AllAuthResponse =
        client.generateRecoveryCodes()

    override suspend fun getProviders(): AllAuthResponse =
        client.getProviders()

    override suspend fun disconnectProvider(providerId: String, accountUid: String): AllAuthResponse =
        client.disconnectProvider(providerId, accountUid)
}
