package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.response.CardDeleteResponse
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.auth.SessionExpiredException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardRepositoryImplTest {

    private val cardService: CardService = mockk()
    private val repository = CardRepositoryImpl(cardService)

    @Test
    fun `모든 카드 삭제 요청을 전달한다`() = runTest {
        coEvery { cardService.deleteAllCards() } returns ApiResponse(
            success = true,
            data = CardDeleteResponse(deletedCount = 0),
        )

        assertEquals(AppResult.Success(Unit), repository.deleteAllCards())
    }

    @Test
    fun `200이어도 실패 envelope면 실패로 전달한다`() = runTest {
        coEvery { cardService.deleteAllCards() } returns ApiResponse<CardDeleteResponse>(
            success = false,
            error = ApiError(code = "CARD_DELETE_FAILED", message = "삭제 실패"),
        )

        val result = repository.deleteAllCards()

        assertEquals(
            "CARD_DELETE_FAILED",
            ((result as AppResult.Failure).throwable as ApiException).code,
        )
    }

    @Test
    fun `실패 envelope의 인증 만료는 세션 만료로 전달한다`() = runTest {
        coEvery { cardService.deleteAllCards() } returns ApiResponse<CardDeleteResponse>(
            success = false,
            error = ApiError(code = "EXPIRED_TOKEN", message = "만료"),
        )

        val result = repository.deleteAllCards()

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    @Test
    fun `성공 응답에 삭제 수가 없으면 실패로 전달한다`() = runTest {
        coEvery { cardService.deleteAllCards() } returns ApiResponse(success = true)

        val result = repository.deleteAllCards()

        assertTrue(result is AppResult.Failure)
    }
}
