package community.openbase.allauth.client.ui

import community.openbase.allauth.client.AllAuthResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingFlowSummaryTest {
    private fun response(status: Int, flows: List<Map<String, Any?>> = emptyList(), errors: List<Map<String, Any?>> = emptyList()) =
        AllAuthResponse(
            httpStatusCode = status,
            body = mapOf(
                "status" to status,
                "data" to mapOf("flows" to flows),
                "meta" to mapOf("is_authenticated" to false, "session_token" to "tok"),
                "errors" to errors,
            ),
        )

    @Test
    fun `code request 401 with pending login_by_code is the code-sent step`() {
        val r = response(
            401,
            flows = listOf(
                mapOf("id" to "login"),
                mapOf("id" to "login_by_code", "is_pending" to true),
                mapOf("id" to "signup"),
            ),
        )
        assertEquals("Code sent. Check your email and enter the code below.", pendingFlowSummary(r))
    }

    @Test
    fun `pending verify_email and mfa_authenticate have their own next steps`() {
        assertEquals(
            "Check your email for a verification link, then sign in.",
            pendingFlowSummary(response(401, flows = listOf(mapOf("id" to "verify_email", "is_pending" to true)))),
        )
        assertEquals(
            "Enter the code from your authenticator.",
            pendingFlowSummary(response(401, flows = listOf(mapOf("id" to "mfa_authenticate", "is_pending" to true)))),
        )
    }

    @Test
    fun `a 401 without a pending flow stays an error`() {
        assertNull(pendingFlowSummary(response(401, flows = listOf(mapOf("id" to "login"), mapOf("id" to "login_by_code")))))
    }

    @Test
    fun `non-401 responses are never pending summaries`() {
        assertNull(pendingFlowSummary(response(200, flows = listOf(mapOf("id" to "login_by_code", "is_pending" to true)))))
        assertNull(pendingFlowSummary(response(400, errors = listOf(mapOf("message" to "Bad email")))))
    }
}
