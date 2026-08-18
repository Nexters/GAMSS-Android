package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.response.CardCalendarResponse
import com.gamss.android.data.remote.card.model.response.CardDeleteResponse
import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.emotion.EmotionCharacter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CardRepositoryImplTest {

    private val cardService: CardService = mockk()
    private val repository = CardRepositoryImpl(cardService)

    @Test
    fun `날짜를 API 형식으로 조회하고 카드 감정을 캐릭터로 매핑한다`() = runTest {
        val date = LocalDate.of(2026, 8, 15)
        coEvery { cardService.getCardsByDate("2026-08-15") } returns ApiResponse(
            success = true,
            data = listOf(cardResponse(emotion = "ANGER")),
        )

        val result = repository.getCardsByDate(date)

        assertTrue(result is AppResult.Success)
        assertEquals(EmotionCharacter.ANGER, (result as AppResult.Success).data.single().character)
        coVerify(exactly = 1) { cardService.getCardsByDate("2026-08-15") }
    }

    @Test
    fun `알 수 없는 감정 카드는 제외하고 유효한 카드는 남긴다`() = runTest {
        coEvery { cardService.getCardsByDate(any()) } returns ApiResponse(
            success = true,
            data = listOf(
                cardResponse(emotion = "UNKNOWN"),
                cardResponse(emotion = "GRUMPY"),
            ),
        )

        val result = repository.getCardsByDate(DATE)

        assertEquals(
            listOf(EmotionCharacter.PRICKLY),
            (result as AppResult.Success).data.map { it.character },
        )
    }

    @Test
    fun `성공 응답이어도 실패 envelope는 실패로 전한다`() = runTest {
        coEvery { cardService.getCardsByDate(any()) } returns ApiResponse(
            success = false,
            error = ApiError(code = "EXPIRED_TOKEN", message = "만료"),
        )

        val result = repository.getCardsByDate(DATE)

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    @Test
    fun `카드 데이터가 없으면 실패로 전한다`() = runTest {
        coEvery { cardService.getCardsByDate(any()) } returns ApiResponse(success = true)

        val result = repository.getCardsByDate(DATE)

        assertTrue((result as AppResult.Failure).throwable is IllegalStateException)
    }

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

    @Test
    fun `월을 API 형식으로 조회하고 하루치 감정 목록을 카드 한 건씩으로 펼친다`() = runTest {
        coEvery { cardService.getCardsByMonth("2026-08") } returns ApiResponse(
            success = true,
            data = listOf(
                CardCalendarResponse(date = "2026-08-15", emotions = listOf("ANGER", "JOY")),
                CardCalendarResponse(date = "2026-08-16", emotions = listOf("GRUMPY")),
            ),
        )

        val result = repository.getCardsByMonth(YearMonth.of(2026, 8))

        assertEquals(
            listOf(
                CardEntry(LocalDate.of(2026, 8, 15), 0, EmotionCharacter.ANGER),
                CardEntry(LocalDate.of(2026, 8, 15), 1, EmotionCharacter.JOY),
                CardEntry(LocalDate.of(2026, 8, 16), 0, EmotionCharacter.PRICKLY),
            ),
            (result as AppResult.Success).data,
        )
        coVerify(exactly = 1) { cardService.getCardsByMonth("2026-08") }
    }

    /** 날짜별 조회도 알 수 없는 감정을 버리므로, 순번은 버린 뒤를 기준으로 세야 두 응답이 맞물린다. */
    @Test
    fun `알 수 없는 감정을 버린 뒤를 기준으로 그날 순번을 센다`() = runTest {
        coEvery { cardService.getCardsByMonth(any()) } returns ApiResponse(
            success = true,
            data = listOf(
                CardCalendarResponse(date = "2026-08-15", emotions = listOf("UNKNOWN", "ANGER")),
            ),
        )

        val result = repository.getCardsByMonth(YearMonth.of(2026, 8))

        assertEquals(
            listOf(CardEntry(LocalDate.of(2026, 8, 15), 0, EmotionCharacter.ANGER)),
            (result as AppResult.Success).data,
        )
    }

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 8, 15)

        fun cardResponse(emotion: String) = CardResponse(
            id = 1L,
            conversationId = 10L,
            emotion = emotion,
            emotionLabel = "분노",
            summary = "회의가 길어졌다",
            message = "오늘 많이 힘들었겠다",
            date = "2026-08-15",
        )
    }
}
