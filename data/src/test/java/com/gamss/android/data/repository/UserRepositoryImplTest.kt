package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.user.UserService
import com.gamss.android.data.remote.user.model.request.UpdateNicknameRequest
import com.gamss.android.data.remote.user.model.response.DailyTokenUsageDataResponse
import com.gamss.android.data.remote.user.model.response.UserInfoResponse
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.user.NicknameUpdateException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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

class UserRepositoryImplTest {

    private val userService: UserService = mockk()
    private val repository = UserRepositoryImpl(userService)

    @Test
    fun `닉네임 변경 성공 응답을 사용자 프로필로 변환한다`() = runTest {
        coEvery {
            userService.updateNickname(UpdateNicknameRequest(nickname = "새닉네임"))
        } returns ApiResponse(
            success = true,
            data = userInfoResponse(nickname = "새닉네임"),
        )

        val result = repository.updateNickname("새닉네임")

        val profile = (result as AppResult.Success).data
        assertEquals(1L, profile.id)
        assertEquals("user@gamss.com", profile.email)
        assertEquals("새닉네임", profile.nickname)
        assertEquals("ACTIVE", profile.status)
        assertEquals("2026-08-05T00:00:00Z", profile.createdAt)
        coVerify(exactly = 1) {
            userService.updateNickname(UpdateNicknameRequest(nickname = "새닉네임"))
        }
    }

    @Test
    fun `서버의 잘못된 입력 코드를 닉네임 누락 실패로 변환한다`() = runTest {
        coEvery { userService.updateNickname(any()) } throws httpException("INVALID_INPUT")

        val result = repository.updateNickname("감쓰")

        assertTrue((result as AppResult.Failure).throwable is NicknameUpdateException.MissingNickname)
    }

    @Test
    fun `서버의 잘못된 닉네임 코드를 유효하지 않은 닉네임 실패로 변환한다`() = runTest {
        coEvery { userService.updateNickname(any()) } throws httpException("INVALID_NICKNAME")

        val result = repository.updateNickname("금칙어")

        assertTrue((result as AppResult.Failure).throwable is NicknameUpdateException.InvalidNickname)
    }

    @Test
    fun `알 수 없는 서버 오류 코드는 HTTP 실패로 유지한다`() = runTest {
        coEvery { userService.updateNickname(any()) } throws httpException("UNKNOWN_ERROR")

        val result = repository.updateNickname("감쓰")

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ApiException.Http)
        assertEquals("UNKNOWN_ERROR", (throwable as ApiException.Http).code)
    }

    @Test
    fun `네트워크 오류는 네트워크 실패로 유지한다`() = runTest {
        val cause = IOException("offline")
        coEvery { userService.updateNickname(any()) } throws cause

        val result = repository.updateNickname("감쓰")

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ApiException.Network)
        assertSame(cause, throwable.cause)
    }

    @Test(expected = CancellationException::class)
    fun `닉네임 변경 취소는 실패로 변환하지 않고 전파한다`() = runTest {
        coEvery { userService.updateNickname(any()) } throws CancellationException()

        repository.updateNickname("감쓰")
    }

    @Test
    fun `일일 토큰 사용량 응답을 도메인 모델로 변환한다`() = runTest {
        coEvery { userService.getDailyTokenUsage() } returns ApiResponse(
            success = true,
            data = DailyTokenUsageDataResponse(
                usedTokens = 12_000,
                dailyLimit = 100_000,
                exceeded = false,
            ),
        )

        val result = repository.getDailyTokenUsage()

        assertEquals(12_000L, (result as AppResult.Success).data.usedTokens)
        assertEquals(100_000L, result.data.dailyLimit)
    }

    @Test
    fun `일일 토큰 사용량 데이터가 없으면 실패로 돌려준다`() = runTest {
        coEvery { userService.getDailyTokenUsage() } returns ApiResponse(success = true)

        val result = repository.getDailyTokenUsage()

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `일일 토큰 사용량 조회의 401 응답은 세션 만료로 변환한다`() = runTest {
        coEvery { userService.getDailyTokenUsage() } throws httpException(UNAUTHORIZED)

        val result = repository.getDailyTokenUsage()

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    private fun httpException(code: String): HttpException {
        val errorBody = """{"success":false,"error":{"code":"$code","message":"failed"}}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(
            Response.error<ApiResponse<UserInfoResponse>>(BAD_REQUEST, errorBody),
        )
    }

    private fun userInfoResponse(nickname: String) = UserInfoResponse(
        id = 1L,
        email = "user@gamss.com",
        nickname = nickname,
        status = "ACTIVE",
        createdAt = "2026-08-05T00:00:00Z",
    )

    private companion object {
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
    }
}
