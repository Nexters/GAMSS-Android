package com.gamss.android.data.di

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.auth.TokenAuthenticator
import com.gamss.android.data.auth.TokenInterceptor
import com.gamss.android.data.local.auth.TokenProvider
import com.gamss.android.domain.auth.AuthRepository
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * NetworkModule.provideOkHttpClient()가 실제로 TokenInterceptor/TokenAuthenticator를
 * 올바르게 엮어서, UserService 등이 쓰는 클라이언트가 401 -> 재발급 -> 재시도를
 * end-to-end로 수행하는지 검증한다. Hilt 그래프 전체를 띄우지 않고, 모듈 함수를
 * 직접 호출해 실제 조립 결과를 확인한다.
 */
class NetworkModuleOkHttpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var authRepository: AuthRepository
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokenProvider = mockk()
        authRepository = mockk()

        client = NetworkModule.provideOkHttpClient(
            tokenInterceptor = TokenInterceptor(tokenProvider),
            tokenAuthenticator = TokenAuthenticator(tokenProvider, Lazy { authRepository }),
            loggingInterceptor = HttpLoggingInterceptor(),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `이 클라이언트로 요청 시 401을 받으면 재발급 후 새 토큰으로 자동 재시도한다`() {
        every { tokenProvider.getAccessToken() } returnsMany
            listOf("expired-token", "expired-token", "refreshed-token")
        coEvery { authRepository.reissueTokens() } returns AppResult.Success(Unit)

        server.enqueue(MockResponse.Builder().code(401).build())
        server.enqueue(MockResponse.Builder().code(200).body("ok").build())

        val response = client.newCall(
            Request.Builder().url(server.url("/api/members/me")).build(),
        ).execute()

        assertEquals(200, response.code)
        response.close()

        val firstRequest = server.takeRequest()
        val secondRequest = server.takeRequest()
        assertEquals("Bearer expired-token", firstRequest.headers["Authorization"])
        assertEquals("Bearer refreshed-token", secondRequest.headers["Authorization"])
    }

    @Test
    fun `재발급이 실패하면 최초 401 응답이 그대로 호출자에게 전달된다`() {
        every { tokenProvider.getAccessToken() } returns "expired-token"
        coEvery { authRepository.reissueTokens() } returns AppResult.Failure(RuntimeException("fail"))

        server.enqueue(MockResponse.Builder().code(401).build())

        val response = client.newCall(
            Request.Builder().url(server.url("/api/members/me")).build(),
        ).execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(1, server.requestCount)
    }
}
