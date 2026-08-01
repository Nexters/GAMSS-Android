package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.model.SessionState
import com.google.firebase.auth.FirebaseAuth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val authService: AuthService = mockk()
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val authTokenLocalDataSource: AuthTokenLocalDataSource = mockk(relaxed = true)

    @Test
    fun `저장된 두 토큰이 모두 없으면 재발급 없이 인증되지 않은 세션을 반환한다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens()
        val repository = repository()

        val result = repository.restoreSession()

        assertTrue(result is AppResult.Success)
        assertEquals(SessionState.Unauthenticated, repository.sessionState.value)
        coVerify(exactly = 0) { authService.reissueTokens(any()) }
        coVerify(exactly = 0) { authTokenLocalDataSource.clearTokens() }
    }

    @Test
    fun `저장된 access token이 있으면 재발급 없이 인증된 세션을 반환한다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )
        val repository = repository()

        val result = repository.restoreSession()

        assertTrue(result is AppResult.Success)
        assertEquals(SessionState.Authenticated, repository.sessionState.value)
        coVerify(exactly = 0) { authService.reissueTokens(any()) }
    }

    @Test
    fun `refresh token만 있으면 토큰 재발급 성공 후 인증된 세션을 반환한다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens(
            refreshToken = "refresh-token",
        )
        coEvery {
            authService.reissueTokens(RefreshTokenRequest(refreshToken = "refresh-token"))
        } returns tokenResponse()
        val repository = repository()

        val result = repository.restoreSession()

        assertTrue(result is AppResult.Success)
        assertEquals(SessionState.Authenticated, repository.sessionState.value)
        coVerify(exactly = 1) {
            authTokenLocalDataSource.saveTokens(
                storedTokens(
                    accessToken = "new-access-token",
                    refreshToken = "new-refresh-token",
                ),
            )
        }
    }

    @Test
    fun `토큰 복호화 실패 시 실패를 반환하고 세션을 무효화한다`() = runTest {
        val decryptionFailure = IllegalStateException("decrypt failed")
        coEvery { authTokenLocalDataSource.getTokens() } throws decryptionFailure
        val repository = repository()

        val result = repository.restoreSession()
        advanceUntilIdle()

        assertTrue(result is AppResult.Failure)
        assertSame(decryptionFailure, (result as AppResult.Failure).throwable)
        assertEquals(SessionState.Unauthenticated, repository.sessionState.value)
        coVerify(exactly = 1) { authTokenLocalDataSource.clearTokens() }
        verify(exactly = 1) { firebaseAuth.signOut() }
    }

    @Test
    fun `재발급이 200이지만 data가 없으면 세션을 무효화한다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens(
            refreshToken = "refresh-token",
        )
        coEvery { authService.reissueTokens(any()) } returns ApiResponse(
            success = false,
            data = null,
        )
        val repository = repository()

        val result = repository.reissueTokens()
        advanceUntilIdle()

        assertTrue(result is AppResult.Failure)
        assertEquals(SessionState.Unauthenticated, repository.sessionState.value)
        coVerify(exactly = 1) { authTokenLocalDataSource.clearTokens() }
    }

    @Test
    fun `재발급이 HTTP 500이면 세션을 무효화한다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens(
            refreshToken = "refresh-token",
        )
        coEvery { authService.reissueTokens(any()) } throws httpException(500)
        val repository = repository()

        val result = repository.reissueTokens()
        advanceUntilIdle()

        assertTrue(result is AppResult.Failure)
        assertEquals(SessionState.Unauthenticated, repository.sessionState.value)
        coVerify(exactly = 1) { authTokenLocalDataSource.clearTokens() }
    }

    @Test
    fun `재발급 네트워크 오류는 세션을 유지한다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )
        val repository = repository()
        repository.restoreSession()

        coEvery { authTokenLocalDataSource.getTokens() } returns storedTokens(
            refreshToken = "refresh-token",
        )
        coEvery { authService.reissueTokens(any()) } throws IOException("offline")

        val result = repository.reissueTokens()
        advanceUntilIdle()

        assertTrue(result is AppResult.Failure)
        assertEquals(SessionState.Authenticated, repository.sessionState.value)
        coVerify(exactly = 0) { authTokenLocalDataSource.clearTokens() }
    }

    @Test(expected = CancellationException::class)
    fun `세션 복원 취소는 실패로 변환하거나 세션을 무효화하지 않는다`() = runTest {
        coEvery { authTokenLocalDataSource.getTokens() } throws CancellationException()
        val repository = repository()

        try {
            repository.restoreSession()
        } finally {
            coVerify(exactly = 0) { authTokenLocalDataSource.clearTokens() }
        }
    }

    private fun kotlinx.coroutines.test.TestScope.repository() = AuthRepositoryImpl(
        authService = authService,
        firebaseAuth = firebaseAuth,
        authTokenLocalDataSource = authTokenLocalDataSource,
        applicationScope = this,
    )

    private fun storedTokens(
        accessToken: String? = null,
        refreshToken: String? = null,
    ) = StoredAuthTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
    )

    private fun tokenResponse() = ApiResponse(
        success = true,
        data = LoginResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
        ),
    )

    private fun httpException(statusCode: Int): HttpException {
        val errorBody = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<ApiResponse<LoginResponse>>(statusCode, errorBody))
    }
}
