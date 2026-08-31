package community.openbase.allauth.client

import community.openbase.allauth.client.storage.InMemoryAllAuthTokenStorage
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AllAuthClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loginStoresSessionAndJwtTokens() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {
                  "status": 200,
                  "meta": {
                    "is_authenticated": true,
                    "session_token": "session-1"
                  },
                  "data": {
                    "access_token": "access-1",
                    "refresh_token": "refresh-1",
                    "user": {"id": 1}
                  }
                }
                """.trimIndent(),
            ),
        )
        val storage = InMemoryAllAuthTokenStorage()
        val client = newClient(storage)

        val response = client.loginWithEmail("person@example.com", "password")
        val request = server.takeRequest()

        assertEquals("/auth/login", request.path)
        assertTrue(response.isAuthenticated)
        assertEquals("session-1", storage.readSessionToken())
        assertEquals("access-1", client.jwtAccessToken)
        assertEquals("refresh-1", storage.readRefreshToken())
    }

    @Test
    fun retriesOnceWithRefreshedJwtAfterUnauthorizedResponse() = runTest {
        server.enqueue(jsonResponse("""{"status":401,"meta":{"is_authenticated":false},"data":{}}""", code = 401))
        server.enqueue(
            jsonResponse(
                """
                {
                  "status": 200,
                  "meta": {"is_authenticated": true},
                  "data": {
                    "access_token": "access-2",
                    "refresh_token": "refresh-2"
                  }
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(jsonResponse("""{"status":200,"meta":{"is_authenticated":true},"data":{"user":{"id":1}}}"""))

        val storage = InMemoryAllAuthTokenStorage(initialRefreshToken = "refresh-1")
        val client = newClient(storage)

        val response = client.getAuth()
        val first = server.takeRequest()
        val refresh = server.takeRequest()
        val retry = server.takeRequest()

        assertEquals("/auth/session", first.path)
        assertEquals("/tokens/refresh", refresh.path)
        assertEquals("/auth/session", retry.path)
        assertEquals("Bearer access-2", retry.getHeader("Authorization"))
        assertTrue(response.isAuthenticated)
        assertEquals("refresh-2", storage.readRefreshToken())
    }

    @Test
    fun rejectedRefreshTokenClearsJwtButKeepsSessionToken() = runTest {
        server.enqueue(jsonResponse("""{"status":401,"meta":{"is_authenticated":false},"data":{}}""", code = 401))
        server.enqueue(jsonResponse("""{"status":400,"errors":[{"message":"bad token"}]}""", code = 400))

        val storage = InMemoryAllAuthTokenStorage(
            initialSessionToken = "session-1",
            initialRefreshToken = "refresh-1",
        )
        val client = newClient(storage)
        client.jwtAccessToken = "access-1"

        runCatching { client.getAuth() }

        assertNull(client.jwtAccessToken)
        assertNull(storage.readRefreshToken())
        assertEquals("session-1", storage.readSessionToken())
    }

    @Test
    fun nonJsonSignupFailureReturnsControlledApiError() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html>internal details must not reach the UI</html>"),
        )
        val client = newClient(InMemoryAllAuthTokenStorage())

        val error = runCatching {
            client.signUp("person@example.com", "password")
        }.exceptionOrNull()

        assertTrue(error is AllAuthException.ApiError)
        assertEquals(
            "Authentication server returned an unexpected response (HTTP 500, text/html).",
            error?.message,
        )
        assertFalse(error?.message.orEmpty().contains("internal details"))
        assertFalse(error?.message.orEmpty().contains("JsonReader"))
    }

    @Test
    fun malformedJsonReturnsGenericInvalidResponse() = runTest {
        server.enqueue(jsonResponse("not-json", code = 502))
        val client = newClient(InMemoryAllAuthTokenStorage())

        val error = runCatching { client.getAuth() }.exceptionOrNull()

        assertTrue(error is AllAuthException.InvalidResponse)
        assertEquals("Invalid response from server", error?.message)
        assertFalse(error?.message.orEmpty().contains("JsonReader"))
    }

    private fun newClient(storage: InMemoryAllAuthTokenStorage): AllAuthClient =
        AllAuthClient(httpClient = OkHttpClient(), tokenStorage = storage).also {
            it.setup(server.url("/").toString().trimEnd('/'))
        }

    private fun jsonResponse(body: String, code: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
}
