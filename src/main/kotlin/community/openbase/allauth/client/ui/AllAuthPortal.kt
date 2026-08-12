package community.openbase.allauth.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import community.openbase.allauth.client.AllAuthResponse
import community.openbase.allauth.client.AuthFlow
import community.openbase.allauth.client.AuthState
import kotlinx.coroutines.launch

internal enum class AuthMode {
    Login,
    Signup,
    Code,
    Reset,
    Verify,
}

/**
 * Reusable AllAuth sign-in portal. Renders password login plus the secondary
 * flows the server advertises (email-code login, password reset, signup, email
 * verification) and surfaces an MFA challenge when the session is pending one.
 *
 * @param actions the auth surface (see [AuthActions]).
 * @param title large heading shown above the forms.
 * @param subtitle supporting copy under the [title].
 */
@Composable
public fun AllAuthPortal(
    actions: AuthActions,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val authState by actions.state.collectAsState()
    var mode by remember { mutableStateOf(AuthMode.Login) }
    val availableModes = AuthMode.entries.filter { item ->
        when (item) {
            AuthMode.Login, AuthMode.Reset, AuthMode.Verify -> true
            AuthMode.Signup -> authState.signupAllowed
            AuthMode.Code -> authState.loginByCodeEnabled
        }
    }
    LaunchedEffect(availableModes) {
        if (mode !in availableModes) {
            mode = AuthMode.Login
        }
    }

    LazyColumn(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(subtitle)
        }

        if (authState.requiresReauthentication || authState.isPending(AuthFlow.MFA_AUTHENTICATE)) {
            item { MfaChallengeCard(actions) }
        }

        item {
            when (mode) {
                AuthMode.Login -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PasswordLoginCard(actions)
                    LoginSecondaryLinks(authState) { nextMode ->
                        if (nextMode in availableModes) {
                            mode = nextMode
                        }
                    }
                }
                AuthMode.Signup -> SignupCard(actions) { mode = AuthMode.Login }
                AuthMode.Code -> LoginCodeCard(actions) { mode = AuthMode.Login }
                AuthMode.Reset -> PasswordResetCard(actions) { mode = AuthMode.Login }
                AuthMode.Verify -> VerifyEmailCard(actions) { mode = AuthMode.Login }
            }
        }
    }
}

@Composable
private fun PasswordLoginCard(actions: AuthActions) {
    val scope = rememberCoroutineScope()
    val authState by actions.state.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<AllAuthResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AuthCard(title = "Sign in", response = response, error = error) {
        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text(if (authState.emailAuthEnabled) "Email or username" else "Username") },
            modifier = Modifier.fillMaxWidth(),
        )
        PasswordField("Password", password) { password = it }
        Button(
            onClick = {
                scope.launch {
                    isSubmitting = true
                    try {
                        error = null
                        response = null
                        runCatching { actions.login(identifier, password) }
                            .onSuccess { actions.initialize() }
                            .onFailure { error = it.message }
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting && identifier.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isSubmitting) "Signing in..." else "Sign in")
        }
    }
}

@Composable
private fun LoginSecondaryLinks(
    authState: AuthState,
    navigate: (AuthMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (authState.loginByCodeEnabled) {
            TextButton(onClick = { navigate(AuthMode.Code) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in with email code")
            }
        }
        TextButton(onClick = { navigate(AuthMode.Reset) }, modifier = Modifier.fillMaxWidth()) {
            Text("Forgot password?")
        }
        if (authState.signupAllowed) {
            TextButton(onClick = { navigate(AuthMode.Signup) }, modifier = Modifier.fillMaxWidth()) {
                Text("Create account")
            }
        }
        TextButton(onClick = { navigate(AuthMode.Verify) }, modifier = Modifier.fillMaxWidth()) {
            Text("Verify email address")
        }
    }
}

@Composable
private fun SignupCard(
    actions: AuthActions,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val authState by actions.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<AllAuthResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthCard(title = "Create account", response = response, error = error) {
        if (authState.emailAuthEnabled) {
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        }
        if (authState.usernameAuthEnabled) {
            OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        }
        PasswordField("Password", password) { password = it }
        Button(
            onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.signUp(email, password, username) }
                        .onSuccess { actions.initialize() }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign up")
        }
        TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Sign in")
        }
    }
}

@Composable
private fun LoginCodeCard(
    actions: AuthActions,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<AllAuthResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthCard(title = "Sign in with code", response = response, error = error) {
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.requestLoginCode(email) }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send code")
        }
        OutlinedTextField(code, { code = it }, label = { Text("Code") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.confirmLoginCode(code) }
                        .onSuccess { actions.initialize() }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Confirm code")
        }
        TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Use password instead")
        }
    }
}

@Composable
private fun PasswordResetCard(
    actions: AuthActions,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<AllAuthResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthCard(title = "Reset password", response = response, error = error) {
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.requestPasswordReset(email) }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send reset link")
        }
        OutlinedTextField(key, { key = it }, label = { Text("Reset key") }, modifier = Modifier.fillMaxWidth())
        PasswordField("New password", password) { password = it }
        PasswordField("Confirm password", confirm) { confirm = it }
        Button(
            onClick = {
                scope.launch {
                    error = null
                    if (password != confirm) {
                        error = "Passwords do not match"
                        return@launch
                    }
                    response = runCatching { actions.resetPassword(key, password) }
                        .onSuccess { actions.initialize() }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset password")
        }
        TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Back to sign in")
        }
    }
}

@Composable
private fun VerifyEmailCard(
    actions: AuthActions,
    onSignIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<AllAuthResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthCard(title = "Verify email", response = response, error = error) {
        OutlinedTextField(key, { key = it }, label = { Text("Verification key") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.getEmailVerification(key) }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            }) {
                Text("Check")
            }
            Button(onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.verifyEmail(key) }
                        .onSuccess { actions.initialize() }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            }) {
                Text("Verify")
            }
        }
        TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Back to sign in")
        }
    }
}

@Composable
private fun MfaChallengeCard(actions: AuthActions) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var response by remember { mutableStateOf<AllAuthResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthCard(title = "Multi-factor authentication", response = response, error = error) {
        OutlinedTextField(code, { code = it }, label = { Text("Authenticator or recovery code") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.authenticateTOTP(code) }
                        .onSuccess { actions.initialize() }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            }) {
                Text("Use TOTP")
            }
            OutlinedButton(onClick = {
                scope.launch {
                    error = null
                    response = runCatching { actions.authenticateWithRecoveryCode(code) }
                        .onSuccess { actions.initialize() }
                        .getOrElse {
                            error = it.message
                            null
                        }
                }
            }) {
                Text("Use recovery")
            }
        }
    }
}
