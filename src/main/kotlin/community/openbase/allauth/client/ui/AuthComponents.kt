package community.openbase.allauth.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import community.openbase.allauth.client.AllAuthResponse
import community.openbase.allauth.client.AuthFlow

/**
 * Card container for a single auth form. Renders a title, the caller's fields,
 * and a trailing status/error summary (mirrors the Swift `AuthForm` + status
 * views).
 */
@Composable
internal fun AuthCard(
    title: String,
    response: AllAuthResponse?,
    error: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
            response?.let { ResponseSummary(it) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** Secure text entry field. Equivalent to the Swift `PasswordField`. */
@Composable
internal fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Compact summary of a response's general errors / success. Swift `StatusView`. */
@Composable
internal fun ResponseSummary(response: AllAuthResponse) {
    val errors = response.generalErrors.joinToString()
    val pending = pendingFlowSummary(response)
    when {
        errors.isNotBlank() -> Text(errors, color = MaterialTheme.colorScheme.error)
        response.isSuccess -> Text("Success.", color = MaterialTheme.colorScheme.primary)
        pending != null -> Text(pending, color = MaterialTheme.colorScheme.primary)
        response.status >= 400 -> Text("Status ${response.status}", color = MaterialTheme.colorScheme.error)
    }
}

/**
 * allauth answers an unfinished flow with HTTP 401 plus `data.flows[].is_pending`:
 * after `auth/code/request` that 401 means "code sent, now confirm it", not a
 * failure (the Swift client applies such responses as the current auth state).
 * Returns the human-readable next step for the first pending flow we know, or
 * null when the 401 carries no pending flow and should be shown as an error.
 */
internal fun pendingFlowSummary(response: AllAuthResponse): String? {
    if (response.status != 401) return null
    return when {
        response.pendingFlow(AuthFlow.LOGIN_BY_CODE) != null ->
            "Code sent. Check your email and enter the code below."
        response.pendingFlow(AuthFlow.VERIFY_EMAIL) != null ->
            "Check your email for a verification link, then sign in."
        response.pendingFlow(AuthFlow.MFA_AUTHENTICATE) != null ->
            "Enter the code from your authenticator."
        else -> null
    }
}

/** A titled row with an optional subtitle and trailing actions. */
@Composable
internal fun AccountRow(
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.ifBlank { "Unknown" }, fontWeight = FontWeight.Medium)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions()
        }
    }
}

internal fun AllAuthResponse?.summaryOr(fallback: String): String {
    val response = this ?: return fallback
    val user = response.user?.entries?.joinToString { "${it.key}: ${it.value}" }
    val data = response.get("data")?.toString()
    return user ?: data ?: "Status ${response.status}"
}

@Suppress("UNCHECKED_CAST")
internal fun AllAuthResponse?.dataMaps(): List<Map<String, Any?>> =
    (this?.get("data") as? List<*>)?.mapNotNull { it as? Map<String, Any?> } ?: emptyList()

internal fun Map<String, Any?>?.stringValue(key: String): String =
    when (val value = this?.get(key)) {
        is String -> value
        is Number, is Boolean -> value.toString()
        else -> ""
    }

internal fun Map<String, Any?>?.boolValue(key: String): Boolean =
    this?.get(key) as? Boolean ?: false

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.mapValue(key: String): Map<String, Any?> =
    (this[key] as? Map<String, Any?>) ?: emptyMap()
