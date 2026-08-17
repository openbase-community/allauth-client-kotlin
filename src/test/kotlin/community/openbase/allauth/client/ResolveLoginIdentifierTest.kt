package community.openbase.allauth.client

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveLoginIdentifierTest {
    @Test
    fun emailOnlyRoutesNonAtInputToEmail() {
        assertEquals(
            LoginIdentifier.EMAIL,
            resolveLoginIdentifier(emailEnabled = true, usernameEnabled = false, identifier = "alice"),
        )
        assertEquals(
            LoginIdentifier.EMAIL,
            resolveLoginIdentifier(emailEnabled = true, usernameEnabled = false, identifier = "alice@example.com"),
        )
    }

    @Test
    fun usernameOnlyAlwaysRoutesToUsername() {
        assertEquals(
            LoginIdentifier.USERNAME,
            resolveLoginIdentifier(emailEnabled = false, usernameEnabled = true, identifier = "alice"),
        )
        assertEquals(
            LoginIdentifier.USERNAME,
            resolveLoginIdentifier(emailEnabled = false, usernameEnabled = true, identifier = "alice@example.com"),
        )
    }

    @Test
    fun bothEnabledKeepsAtHeuristic() {
        assertEquals(
            LoginIdentifier.EMAIL,
            resolveLoginIdentifier(emailEnabled = true, usernameEnabled = true, identifier = "alice@example.com"),
        )
        assertEquals(
            LoginIdentifier.USERNAME,
            resolveLoginIdentifier(emailEnabled = true, usernameEnabled = true, identifier = "alice"),
        )
    }

    @Test
    fun unloadedOrEmptyConfigDefaultsToEmail() {
        // AuthState with no config derives emailAuthEnabled=true / usernameAuthEnabled=false,
        // but guard the truly-empty (neither) case too.
        assertEquals(
            LoginIdentifier.EMAIL,
            resolveLoginIdentifier(emailEnabled = false, usernameEnabled = false, identifier = "alice"),
        )
    }
}
