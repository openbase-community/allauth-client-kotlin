package community.openbase.allauth.client

public class AllAuthUrls(baseUrl: String) {
    public val baseUrl: String = baseUrl.trimEnd('/')

    public val config: String get() = "$baseUrl/config"

    public val changePassword: String get() = "$baseUrl/account/password/change"
    public val emailAddresses: String get() = "$baseUrl/account/email"
    public val authenticators: String get() = "$baseUrl/account/authenticators"
    public val totpAuthenticator: String get() = "$baseUrl/account/authenticators/totp"
    public val recoveryCodesAuthenticator: String get() = "$baseUrl/account/authenticators/recovery-codes"
    public val webauthnAuthenticator: String get() = "$baseUrl/account/authenticators/webauthn"
    public val providers: String get() = "$baseUrl/account/providers"

    public val session: String get() = "$baseUrl/auth/session"
    public val tokenRefresh: String get() = "$baseUrl/tokens/refresh"
    public val login: String get() = "$baseUrl/auth/login"
    public val reauthenticate: String get() = "$baseUrl/auth/reauthenticate"
    public val requestLoginCode: String get() = "$baseUrl/auth/code/request"
    public val confirmLoginCode: String get() = "$baseUrl/auth/code/confirm"
    public val signup: String get() = "$baseUrl/auth/signup"
    public val verifyEmail: String get() = "$baseUrl/auth/email/verify"
    public val requestPasswordReset: String get() = "$baseUrl/auth/password/request"
    public val resetPassword: String get() = "$baseUrl/auth/password/reset"

    public val mfaAuthenticate: String get() = "$baseUrl/auth/2fa/authenticate"
    public val mfaReauthenticate: String get() = "$baseUrl/auth/2fa/reauthenticate"
    public val mfaTrust: String get() = "$baseUrl/auth/2fa/trust"
    public val webauthnAuthenticate: String get() = "$baseUrl/auth/webauthn/authenticate"
    public val webauthnReauthenticate: String get() = "$baseUrl/auth/webauthn/reauthenticate"
    public val webauthnLogin: String get() = "$baseUrl/auth/webauthn/login"
    public val webauthnSignup: String get() = "$baseUrl/auth/webauthn/signup"

    public val providerRedirect: String get() = "$baseUrl/auth/provider/redirect"
    public val providerToken: String get() = "$baseUrl/auth/provider/token"
    public val providerSignup: String get() = "$baseUrl/auth/provider/signup"

    public val sessions: String get() = "$baseUrl/auth/sessions"
}

