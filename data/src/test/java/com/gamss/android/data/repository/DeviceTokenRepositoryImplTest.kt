package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.push.DeviceTokenService
import com.gamss.android.data.remote.push.model.request.RegisterDeviceTokenRequest
import com.gamss.android.data.remote.push.model.request.UnregisterDeviceTokenRequest
import com.gamss.android.domain.auth.SessionExpiredException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DeviceTokenRepositoryImplTest {

    private val deviceTokenService: DeviceTokenService = mockk()
    private val repository = DeviceTokenRepositoryImpl(deviceTokenService)

    @Test
    fun `토큰 등록에 성공하면 성공을 반환한다`() = runTest {
        coEvery {
            deviceTokenService.register(RegisterDeviceTokenRequest(token = "token-123"))
        } returns ApiResponse(success = true)

        val result = repository.registerToken("token-123")

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) {
            deviceTokenService.register(RegisterDeviceTokenRequest(token = "token-123"))
        }
    }

    @Test
    fun `토큰 해제에 성공하면 성공을 반환한다`() = runTest {
        coEvery {
            deviceTokenService.unregister(UnregisterDeviceTokenRequest(token = "token-123"))
        } returns ApiResponse(success = true)

        val result = repository.unregisterToken("token-123")

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) {
            deviceTokenService.unregister(UnregisterDeviceTokenRequest(token = "token-123"))
        }
    }

    @Test
    fun `401 응답은 세션 만료로 변환한다`() = runTest {
        coEvery { deviceTokenService.register(any()) } throws httpException(UNAUTHORIZED)

        val result = repository.registerToken("token-123")

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    @Test
    fun `알 수 없는 토큰 오류는 HTTP 실패로 유지한다`() = runTest {
        coEvery { deviceTokenService.register(any()) } throws httpException("INVALID_DEVICE_TOKEN")

        val result = repository.registerToken("x".repeat(600))

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ApiException.Http)
        assertEquals("INVALID_DEVICE_TOKEN", (throwable as ApiException.Http).code)
    }

    @Test(expected = CancellationException::class)
    fun `등록 취소는 실패로 변환하지 않고 전파한다`() = runTest {
        coEvery { deviceTokenService.register(any()) } throws CancellationException()

        repository.registerToken("token-123")
    }

    private fun httpException(code: String): HttpException {
        val errorBody = """{"success":false,"error":{"code":"$code","message":"failed"}}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(
            Response.error<ApiResponse<Unit>>(BAD_REQUEST, errorBody),
        )
    }

    private companion object {
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
    }
}
