package com.gamss.android.data.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.local.auth.TokenProvider
import com.gamss.android.domain.auth.AuthRepository
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAuthenticatorTest {

    private val tokenProvider: TokenProvider = mockk()
    private val authRepository: AuthRepository = mockk()
    private val authenticator = TokenAuthenticator(tokenProvider, Lazy { authRepository })

    private fun request(accessToken: String? = null): Request {
        val builder = Request.Builder().url("https://example.invalid/api/test")
        if (accessToken != null) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        return builder.build()
    }

    private fun unauthorizedResponse(req: Request, prior: Response? = null): Response =
        Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { if (prior != null) priorResponse(prior) }
            .build()

    @Test
    fun `재발급 토큰으로 재시도한 요청이 다시 401이면 더 이상 시도하지 않고 null을 반환한다`() {
        val req = request("refreshed-token")
        val firstFailure = unauthorizedResponse(request("expired-token"))
        val secondFailure = unauthorizedResponse(req, prior = firstFailure)

        val result = authenticator.authenticate(null, secondFailure)

        assertNull(result)
        coVerify(exactly = 0) { authRepository.reissueTokens() }
    }

    @Test
    fun `다른 스레드가 이미 재발급을 완료했다면 재발급을 다시 호출하지 않고 캐시된 토큰으로 재시도한다`() {
        val req = request("old-token")
        every { tokenProvider.getAccessToken() } returns "new-token"

        val result = authenticator.authenticate(null, unauthorizedResponse(req))

        assertEquals("Bearer new-token", result?.header("Authorization"))
        coVerify(exactly = 0) { authRepository.reissueTokens() }
    }

    @Test
    fun `재발급 성공 시 새 토큰으로 재시도 요청을 만든다`() {
        val req = request("old-token")
        every { tokenProvider.getAccessToken() } returnsMany listOf("old-token", "refreshed-token")
        coEvery { authRepository.reissueTokens() } returns AppResult.Success(Unit)

        val result = authenticator.authenticate(null, unauthorizedResponse(req))

        assertEquals("Bearer refreshed-token", result?.header("Authorization"))
    }

    @Test
    fun `재발급 성공했지만 캐시된 토큰이 여전히 없으면 재시도하지 않는다`() {
        val req = request("old-token")
        every { tokenProvider.getAccessToken() } returnsMany listOf("old-token", null)
        coEvery { authRepository.reissueTokens() } returns AppResult.Success(Unit)

        val result = authenticator.authenticate(null, unauthorizedResponse(req))

        assertNull(result)
    }

    @Test
    fun `재발급 실패 시 null을 반환한다`() {
        val req = request("old-token")
        every { tokenProvider.getAccessToken() } returns "old-token"
        coEvery { authRepository.reissueTokens() } returns AppResult.Failure(RuntimeException("fail"))

        val result = authenticator.authenticate(null, unauthorizedResponse(req))

        assertNull(result)
    }

    @Test
    fun `Authorization 헤더가 없던 요청도 재발급을 시도하고 실패하면 null을 반환한다`() {
        val req = request(accessToken = null)
        // 캐시된 토큰도 없어야(null) failedAccessToken(null)과 같아 재발급 분기를 탄다.
        every { tokenProvider.getAccessToken() } returns null
        coEvery { authRepository.reissueTokens() } returns AppResult.Failure(RuntimeException("fail"))

        val result = authenticator.authenticate(null, unauthorizedResponse(req))

        assertNull(result)
        coVerify(exactly = 1) { authRepository.reissueTokens() }
    }
}
