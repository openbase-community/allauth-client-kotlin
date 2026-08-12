package community.openbase.allauth.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import community.openbase.allauth.client.AllAuthResponse
import kotlinx.coroutines.launch

/**
 * Reusable account-management surface: email addresses, password, MFA (TOTP +
 * recovery codes), active sessions, and connected social accounts. Driven
 * entirely by [AuthActions].
 *
 * WebAuthn / passkey management is intentionally not surfaced here; it requires
 * platform credential plumbing and is exposed only through the client API.
 */
@Composable
public fun AllAuthAccountManagement(
    actions: AuthActions,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var emailResponse by remember { mutableStateOf<AllAuthResponse?>(null) }
    var sessionResponse by remember { mutableStateOf<AllAuthResponse?>(null) }
    var mfaResponse by remember { mutableStateOf<AllAuthResponse?>(null) }
    var providerResponse by remember { mutableStateOf<AllAuthResponse?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var newEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var reauthPassword by remember { mutableStateOf("") }
    var sessionIds by remember { mutableStateOf("") }
    var providerId by remember { mutableStateOf("") }
    var accountUid by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        emailResponse = runCatching { actions.getEmailAddresses() }.getOrNull()
        sessionResponse = runCatching { actions.getSessions() }.getOrNull()
        mfaResponse = runCatching { actions.getAuthenticators() }.getOrNull()
        providerResponse = runCatching { actions.getProviders() }.getOrNull()
    }

    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Account", fontWeight = FontWeight.SemiBold)
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Text("Email addresses", style = MaterialTheme.typography.titleSmall)
            if (emailResponse.dataMaps().isEmpty()) {
                Text(emailResponse.summaryOr("No email data loaded."))
            }
            emailResponse.dataMaps().forEach { email ->
                AccountRow(
                    title = email.stringValue("email"),
                    subtitle = listOfNotNull(
                        if (email.boolValue("primary")) "Primary" else null,
                        if (email.boolValue("verified")) "Verified" else "Unverified",
                    ).joinToString(" • "),
                ) {
                    if (!email.boolValue("primary")) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { actions.setPrimaryEmailAddress(email.stringValue("email")) }
                                    .onSuccess {
                                        status = "Primary email updated."
                                        emailResponse = actions.getEmailAddresses()
                                    }
                                    .onFailure { status = it.message }
                            }
                        }) { Text("Primary") }
                    }
                    if (!email.boolValue("verified")) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { actions.requestEmailVerification(email.stringValue("email")) }
                                    .onSuccess { status = "Verification requested." }
                                    .onFailure { status = it.message }
                            }
                        }) { Text("Verify") }
                    }
                    if (!email.boolValue("primary")) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { actions.deleteEmailAddress(email.stringValue("email")) }
                                    .onSuccess {
                                        status = "Email removed."
                                        emailResponse = actions.getEmailAddresses()
                                    }
                                    .onFailure { status = it.message }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
            OutlinedTextField(newEmail, { newEmail = it }, label = { Text("Add or verify email") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        runCatching { actions.addEmailAddress(newEmail) }
                            .onSuccess {
                                status = "Email added."
                                emailResponse = actions.getEmailAddresses()
                            }
                            .onFailure { status = it.message }
                    }
                }) { Text("Add") }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { actions.setPrimaryEmailAddress(newEmail) }
                            .onSuccess {
                                status = "Primary email updated."
                                emailResponse = actions.getEmailAddresses()
                            }
                            .onFailure { status = it.message }
                    }
                }) { Text("Primary") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { actions.requestEmailVerification(newEmail) }
                            .onSuccess { status = "Verification requested." }
                            .onFailure { status = it.message }
                    }
                }) { Text("Verify") }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { actions.deleteEmailAddress(newEmail) }
                            .onSuccess {
                                status = "Email removed."
                                emailResponse = actions.getEmailAddresses()
                            }
                            .onFailure { status = it.message }
                    }
                }) { Text("Delete") }
            }

            Text("Password", style = MaterialTheme.typography.titleSmall)
            PasswordField("Current password", currentPassword) { currentPassword = it }
            PasswordField("New password", newPassword) { newPassword = it }
            Button(onClick = {
                scope.launch {
                    runCatching { actions.changePassword(currentPassword, newPassword) }
                        .onSuccess { status = "Password changed." }
                        .onFailure { status = it.message }
                }
            }) { Text("Change password") }
            PasswordField("Reauthenticate password", reauthPassword) { reauthPassword = it }
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { actions.reauthenticate(reauthPassword) }
                        .onSuccess { status = "Reauthenticated." }
                        .onFailure { status = it.message }
                }
            }) { Text("Reauthenticate") }

            Text("MFA", style = MaterialTheme.typography.titleSmall)
            if (mfaResponse.dataMaps().isEmpty()) {
                Text(mfaResponse.summaryOr("No MFA data loaded."))
            }
            mfaResponse.dataMaps().forEach { authenticator ->
                val type = authenticator.stringValue("type").ifBlank { "Authenticator" }
                val detail = when (type) {
                    "totp" -> "Authenticator app enabled"
                    "recovery_codes" -> "${authenticator.stringValue("unused_code_count")} of ${authenticator.stringValue("total_code_count")} recovery codes unused"
                    "webauthn" -> authenticator.stringValue("name").ifBlank { "Passkey or security key" }
                    else -> authenticator.toString()
                }
                AccountRow(title = type, subtitle = detail)
            }
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { actions.getTOTPAuthenticator() }
                        .onSuccess { mfaResponse = it }
                        .onFailure { status = it.message }
                }
            }) { Text("Load TOTP setup") }
            OutlinedTextField(totpCode, { totpCode = it }, label = { Text("TOTP code") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        runCatching { actions.activateTOTP(totpCode) }
                            .onSuccess {
                                status = "TOTP updated."
                                mfaResponse = actions.getAuthenticators()
                            }
                            .onFailure { status = it.message }
                    }
                }) { Text("Activate") }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { actions.deactivateTOTP() }
                            .onSuccess {
                                status = "TOTP disabled."
                                mfaResponse = actions.getAuthenticators()
                            }
                            .onFailure { status = it.message }
                    }
                }) { Text("Disable") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { actions.getRecoveryCodes() }
                            .onSuccess { mfaResponse = it }
                            .onFailure { status = it.message }
                    }
                }) { Text("Recovery codes") }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { actions.generateRecoveryCodes() }
                            .onSuccess { mfaResponse = it }
                            .onFailure { status = it.message }
                    }
                }) { Text("Regenerate") }
            }

            Text("Sessions", style = MaterialTheme.typography.titleSmall)
            if (sessionResponse.dataMaps().isEmpty()) {
                Text(sessionResponse.summaryOr("No sessions loaded."))
            }
            sessionResponse.dataMaps().forEach { session ->
                AccountRow(
                    title = session.stringValue("user_agent").ifBlank { session.stringValue("id").ifBlank { "Session" } },
                    subtitle = listOfNotNull(
                        session.stringValue("ip").takeIf { it.isNotBlank() },
                        session.stringValue("last_seen_at").takeIf { it.isNotBlank() }?.let { "Last seen $it" },
                        if (session.boolValue("is_current")) "This device" else null,
                    ).joinToString(" • "),
                ) {
                    if (!session.boolValue("is_current")) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { actions.deleteSessions(listOf(session.stringValue("id"))) }
                                    .onSuccess {
                                        status = "Session deleted."
                                        sessionResponse = actions.getSessions()
                                    }
                                    .onFailure { status = it.message }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
            OutlinedTextField(
                sessionIds,
                { sessionIds = it },
                label = { Text("Session ids, comma separated") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = {
                scope.launch {
                    val ids = sessionIds.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    runCatching { actions.deleteSessions(ids) }
                        .onSuccess {
                            status = "Sessions deleted."
                            sessionResponse = actions.getSessions()
                        }
                        .onFailure { status = it.message }
                }
            }) { Text("Delete sessions") }

            Text("Social accounts", style = MaterialTheme.typography.titleSmall)
            if (providerResponse.dataMaps().isEmpty()) {
                Text(providerResponse.summaryOr("No provider data loaded."))
            }
            providerResponse.dataMaps().forEach { account ->
                val provider = account.mapValue("provider")
                val resolvedProviderId = provider.stringValue("id").ifBlank { account.stringValue("provider") }
                val resolvedUid = account.stringValue("uid")
                AccountRow(
                    title = provider.stringValue("name").ifBlank { resolvedProviderId.ifBlank { "Provider account" } },
                    subtitle = account.stringValue("display").ifBlank { resolvedUid },
                ) {
                    if (resolvedProviderId.isNotBlank() && resolvedUid.isNotBlank()) {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { actions.disconnectProvider(resolvedProviderId, resolvedUid) }
                                    .onSuccess {
                                        status = "Provider disconnected."
                                        providerResponse = actions.getProviders()
                                    }
                                    .onFailure { status = it.message }
                            }
                        }) { Text("Disconnect") }
                    }
                }
            }
            OutlinedTextField(providerId, { providerId = it }, label = { Text("Provider id") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(accountUid, { accountUid = it }, label = { Text("Account uid") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { actions.disconnectProvider(providerId, accountUid) }
                        .onSuccess {
                            status = "Provider disconnected."
                            providerResponse = actions.getProviders()
                        }
                        .onFailure { status = it.message }
                }
            }) { Text("Disconnect provider") }
            Text("WebAuthn passkeys require platform credential plumbing and are exposed through the client API.")
        }
    }
}
