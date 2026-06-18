package community.openbase.allauth.client

import org.junit.Assert.assertEquals
import org.junit.Test

class AllAuthUrlsTest {
    @Test
    fun trimsTrailingSlashAndBuildsKnownEndpoints() {
        val urls = AllAuthUrls("https://example.com/_allauth/app/v1/")

        assertEquals("https://example.com/_allauth/app/v1", urls.baseUrl)
        assertEquals("https://example.com/_allauth/app/v1/config", urls.config)
        assertEquals("https://example.com/_allauth/app/v1/auth/session", urls.session)
        assertEquals("https://example.com/_allauth/app/v1/tokens/refresh", urls.tokenRefresh)
        assertEquals("https://example.com/_allauth/app/v1/auth/password/reset", urls.resetPassword)
    }
}

