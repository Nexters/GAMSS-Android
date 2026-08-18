package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.response.CardDeleteResponse
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.emotion.EmotionCharacter
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

    @Test
    fun `카드 한 장 삭제 요청을 전달한다`() = runTest {
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

    @Test
    fun `감정별 카드 삭제 요청을 전달한다`() = runTest {
        coEvery { cardService.deleteCardsByEmotion("ANGER") } returns ApiResponse(
            success = true,
            data = CardDeleteResponse(deletedCount = 0),
        )

        assertEquals(AppResult.Success(Unit), repository.deleteCardsByEmotion(EmotionCharacter.ANGER))
    }

    @Test
    fun `감정별 삭제 성공 응답에 삭제 수가 없으면 실패로 전달한다`() = runTest {
        coEvery { cardService.deleteCardsByEmotion("ANGER") } returns ApiResponse(success = true)

        val result = repository.deleteCardsByEmotion(EmotionCharacter.ANGER)

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `감정별 삭제 실패 envelope는 실패로 전달한다`() = runTest {
        coEvery { cardService.deleteCardsByEmotion("ANGER") } returns ApiResponse<CardDeleteResponse>(
            success = false,
            error = ApiError(code = "INVALID_INPUT", message = "지원하지 않는 감정"),
        )

        val result = repository.deleteCardsByEmotion(EmotionCharacter.ANGER)

        assertEquals(
            "INVALID_INPUT",
            ((result as AppResult.Failure).throwable as ApiException).code,
        )
    }
}
