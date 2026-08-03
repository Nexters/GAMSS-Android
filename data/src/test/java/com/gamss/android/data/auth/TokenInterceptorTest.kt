package com.gamss.android.data.auth

import com.gamss.android.data.local.auth.TokenProvider
import io.mockk.every
import io.mockk.mockk
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokenProvider = mockk()
        client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenProvider))
            .build()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `accessToken이 있으면 Authorization 헤더를 붙인다`() {
        every { tokenProvider.getAccessToken() } returns "access-token-123"
        server.enqueue(MockResponse.Builder().code(200).build())

        client.newCall(Request.Builder().url(server.url("/any")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer access-token-123", recorded.headers["Authorization"])
    }

    @Test
    fun `accessToken이 null이면 Authorization 헤더를 붙이지 않는다`() {
        every { tokenProvider.getAccessToken() } returns null
        server.enqueue(MockResponse.Builder().code(200).build())

        client.newCall(Request.Builder().url(server.url("/any")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun `accessToken이 빈 문자열이면 Authorization 헤더를 붙이지 않는다`() {
        every { tokenProvider.getAccessToken() } returns ""
        server.enqueue(MockResponse.Builder().code(200).build())

        client.newCall(Request.Builder().url(server.url("/any")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }
}
