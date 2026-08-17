package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.data.remote.model.response.ApiResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CardRepositoryImplTest {

    private val cardService: CardService = mockk()
    private val repository = CardRepositoryImpl(cardService)

    @Test
    fun `카드 삭제 요청을 전달한다`() = runTest {
        coEvery { cardService.deleteCard(1L) } returns ApiResponse(success = true, data = Unit)

        assertEquals(AppResult.Success(Unit), repository.deleteCard(1L))
    }

    @Test
    fun `이미 삭제된 카드는 성공으로 전달한다`() = runTest {
        coEvery { cardService.deleteCard(1L) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "CARD_ALREADY_DELETED", message = "이미 삭제됨"),
        )

        assertEquals(AppResult.Success(Unit), repository.deleteCard(1L))
    }

    @Test
    fun `카드 삭제 실패 envelope는 실패로 전달한다`() = runTest {
        coEvery { cardService.deleteCard(1L) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "CARD_NOT_FOUND", message = "존재하지 않음"),
        )

        val result = repository.deleteCard(1L)

        assertEquals(
            "CARD_NOT_FOUND",
            ((result as AppResult.Failure).throwable as ApiException).code,
        )
    }
}
