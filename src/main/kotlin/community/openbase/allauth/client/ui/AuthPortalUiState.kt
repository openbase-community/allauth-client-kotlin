package community.openbase.allauth.client.ui

import androidx.compose.runtime.saveable.Saver

internal enum class AuthMode {
    Login,
    Signup,
    Code,
    Reset,
    Verify,
}

internal data class AuthPortalUiState(
    val mode: AuthMode = AuthMode.Login,
    val loginIdentifier: String = "",
) {
    val handlesSystemBack: Boolean
        get() = mode != AuthMode.Login

    fun navigateTo(nextMode: AuthMode): AuthPortalUiState = copy(mode = nextMode)

    fun updateLoginIdentifier(identifier: String): AuthPortalUiState =
        copy(loginIdentifier = identifier)

    fun navigateBack(): AuthPortalUiState = copy(mode = AuthMode.Login)

    companion object {
        val Saver: Saver<AuthPortalUiState, List<String>> = Saver(
            save = { listOf(it.mode.name, it.loginIdentifier) },
            restore = { saved ->
                AuthPortalUiState(
                    mode = AuthMode.valueOf(saved[0]),
                    loginIdentifier = saved[1],
                )
            },
        )
    }
}
