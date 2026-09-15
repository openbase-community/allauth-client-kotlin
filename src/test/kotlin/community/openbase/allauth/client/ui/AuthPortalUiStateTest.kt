package community.openbase.allauth.client.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPortalUiStateTest {
    @Test
    fun returningFromCreateAccountPreservesLoginIdentifier() {
        val state = AuthPortalUiState()
            .updateLoginIdentifier("field-user")
            .navigateTo(AuthMode.Signup)
            .navigateBack()

        assertEquals(AuthMode.Login, state.mode)
        assertEquals("field-user", state.loginIdentifier)
    }

    @Test
    fun systemBackFromSecondaryModeReturnsToLoginAndPreservesIdentifier() {
        val signup = AuthPortalUiState(loginIdentifier = "field-user")
            .navigateTo(AuthMode.Signup)

        assertTrue(signup.handlesSystemBack)
        val login = signup.navigateBack()
        assertEquals(AuthMode.Login, login.mode)
        assertEquals("field-user", login.loginIdentifier)
    }

    @Test
    fun systemBackRemainsAvailableToHostFromLoginMode() {
        assertFalse(AuthPortalUiState().handlesSystemBack)
    }

    @Test
    fun pendingEmailVerificationMovesToVerifyAndPrefillsLogin() {
        val pending = AuthPortalUiState().showPendingEmailVerification("field@example.com")

        assertEquals(AuthMode.Verify, pending.mode)
        assertEquals("field@example.com", pending.loginIdentifier)
        assertEquals("field@example.com", pending.pendingVerificationEmail)

        val login = pending.navigateBack()
        assertEquals(AuthMode.Login, login.mode)
        assertEquals("field@example.com", login.loginIdentifier)
        assertEquals(null, login.pendingVerificationEmail)
    }
}
