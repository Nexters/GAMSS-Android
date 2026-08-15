package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.card.CardService
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
    fun `카드 ID로 삭제 요청을 전달한다`() = runTest {
        coEvery { cardService.deleteCard(CARD_ID) } returns ApiResponse(
            success = true,
        )

        assertEquals(AppResult.Success(Unit), repository.deleteCard(CARD_ID))
    }

    @Test
    fun `200이어도 실패 envelope면 실패로 전달한다`() = runTest {
        coEvery { cardService.deleteCard(CARD_ID) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "CARD_DELETE_FAILED", message = "삭제 실패"),
        )

        val result = repository.deleteCard(CARD_ID)

        assertEquals(
            "CARD_DELETE_FAILED",
            ((result as AppResult.Failure).throwable as ApiException).code,
        )
    }

    @Test
    fun `실패 envelope의 인증 만료는 세션 만료로 전달한다`() = runTest {
        coEvery { cardService.deleteCard(CARD_ID) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "EXPIRED_TOKEN", message = "만료"),
        )

        val result = repository.deleteCard(CARD_ID)

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    @Test
    fun `이미 삭제된 카드 응답은 삭제 완료로 처리한다`() = runTest {
        coEvery { cardService.deleteCard(CARD_ID) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "CARD_ALREADY_DELETED", message = "이미 삭제됨"),
        )

        assertEquals(AppResult.Success(Unit), repository.deleteCard(CARD_ID))
    }

    private companion object {
        const val CARD_ID = 42L
    }
}
