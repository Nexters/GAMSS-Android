package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.token.TokenUsageService
import com.gamss.android.data.remote.token.model.response.DailyTokenUsageDataResponse
import com.gamss.android.domain.model.SessionExpiredException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DailyTokenUsageRepositoryImplTest {

    private val tokenUsageService: TokenUsageService = mockk()

    @Test
    fun `token usage data is mapped to the domain model`() = runTest {
        coEvery { tokenUsageService.getDailyTokenUsage() } returns ApiResponse(
            success = true,
            data = DailyTokenUsageDataResponse(
                usedTokens = 12_000,
                dailyLimit = 100_000,
                exceeded = false,
            ),
        )

        val result = repository().getDailyTokenUsage()

        assertTrue(result is AppResult.Success)
        assertEquals(12_000, (result as AppResult.Success).data.usedTokens)
        assertEquals(100_000L, result.data.dailyLimit)
    }

    @Test
    fun `missing data is returned as failure`() = runTest {
        coEvery { tokenUsageService.getDailyTokenUsage() } returns ApiResponse(success = true)

        val result = repository().getDailyTokenUsage()

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `unauthorized response is mapped to session expiration`() = runTest {
        coEvery { tokenUsageService.getDailyTokenUsage() } throws httpException(401)

        val result = repository().getDailyTokenUsage()

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    private fun repository() = DailyTokenUsageRepositoryImpl(tokenUsageService)

    private fun httpException(statusCode: Int): HttpException {
        val errorBody = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<ApiResponse<DailyTokenUsageDataResponse>>(statusCode, errorBody))
    }
}
