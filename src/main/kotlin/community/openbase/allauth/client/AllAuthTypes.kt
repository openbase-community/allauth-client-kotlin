package community.openbase.allauth.client

public enum class AuthFlow(public val id: String) {
    LOGIN("login"),
    LOGIN_BY_CODE("login_by_code"),
    SIGNUP("signup"),
    VERIFY_EMAIL("verify_email"),
    PROVIDER_REDIRECT("provider_redirect"),
    PROVIDER_SIGNUP("provider_signup"),
    MFA_AUTHENTICATE("mfa_authenticate"),
    MFA_REAUTHENTICATE("mfa_reauthenticate"),
    REAUTHENTICATE("reauthenticate"),
    MFA_TRUST("mfa_trust"),
    MFA_WEBAUTHN_SIGNUP("mfa_webauthn_signup"),
    PASSWORD_RESET_BY_CODE("password_reset_by_code"),
}

public enum class AuthenticatorType(public val id: String) {
    TOTP("totp"),
    RECOVERY_CODES("recovery_codes"),
    WEBAUTHN("webauthn"),
}

public enum class AuthProcess(public val id: String) {
    LOGIN("login"),
    CONNECT("connect"),
}

public enum class AuthChangeEvent {
    LOGGED_IN,
    LOGGED_OUT,
    REAUTHENTICATED,
    REAUTHENTICATION_REQUIRED,
    FLOW_UPDATED,
}

internal fun detectAuthChangeEvent(
    previousAuth: AllAuthResponse?,
    currentAuth: AllAuthResponse?,
): AuthChangeEvent? {
    val current = currentAuth ?: return null
    val wasAuthenticated = previousAuth?.isAuthenticated ?: false
    val isNowAuthenticated = current.isAuthenticated

    return when {
        !wasAuthenticated && isNowAuthenticated && current.status == 200 -> AuthChangeEvent.LOGGED_IN
        wasAuthenticated && !isNowAuthenticated && current.status == 401 -> AuthChangeEvent.LOGGED_OUT
        current.requiresReauthentication -> AuthChangeEvent.REAUTHENTICATION_REQUIRED
        current.status == 200 && current.get("data", "flows") != null -> AuthChangeEvent.FLOW_UPDATED
        else -> null
    }
}

public enum class LoginIdentifier {
    EMAIL,
    USERNAME,
}

public sealed class AllAuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    public class InvalidUrl(cause: Throwable? = null) : AllAuthException("Invalid URL", cause)
    public class InvalidResponse(cause: Throwable? = null) : AllAuthException("Invalid response from server", cause)
    public class SessionExpired : AllAuthException("Session expired. Please log in again.")
    public class ApiError(message: String, cause: Throwable? = null) : AllAuthException(message, cause)
}

