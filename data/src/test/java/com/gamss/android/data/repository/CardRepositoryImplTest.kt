package com.gamss.android.data.repository

import android.util.Log
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.local.card.CardLocalDataSource
import com.gamss.android.data.local.card.model.CardEntity
import com.gamss.android.data.local.card.model.toDomain
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.response.CardDeleteResponse
import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.emotion.EmotionCharacter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CardRepositoryImplTest {

    private val cardService: CardService = mockk()
    private val cardLocalDataSource: CardLocalDataSource = mockk()
    private val repository = CardRepositoryImpl(cardService, cardLocalDataSource)

    @Test
    fun `날짜를 API 형식으로 조회하고 카드 감정을 캐릭터로 매핑한다`() = runTest {
        val date = LocalDate.of(2026, 8, 15)
        coEvery { cardService.getCardsByDate("2026-08-15") } returns ApiResponse(
            success = true,
            data = listOf(cardResponse(emotion = "ANGER")),
        )
        coEvery { cardLocalDataSource.upsertAll(any()) } returns Unit

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
        coEvery { cardLocalDataSource.upsertAll(any()) } returns Unit

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

    /**
     * 캐시는 (감정, 달) 단위로만 채워진다. 날짜로 걸러 읽으면 그 날이 다 들어 있다는 보장이 없어,
     * 한 장만 있어도 완전하다고 오해하고 서버를 건너뛴다.
     */
    @Test
    fun `날짜별 조회는 캐시를 읽지도 쓰지도 않는다`() = runTest {
        coEvery { cardService.getCardsByDate(any()) } returns ApiResponse(
            success = true,
            data = listOf(cardResponse(emotion = "ANGER")),
        )

        repository.getCardsByDate(DATE)

        coVerify(exactly = 1) { cardService.getCardsByDate("2026-08-15") }
        coVerify(exactly = 0) { cardLocalDataSource.upsertAll(any()) }
    }

    @Test
    fun `모든 카드 삭제 요청을 전달한다`() = runTest {
        coEvery { cardService.deleteAllCards() } returns ApiResponse(
            success = true,
            data = CardDeleteResponse(deletedCount = 0),
        )
        coEvery { cardLocalDataSource.deleteAll() } returns Unit

        assertEquals(AppResult.Success(Unit), repository.deleteAllCards())
        coVerify(exactly = 1) { cardLocalDataSource.deleteAll() }
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
    fun `카드 한 장 삭제 요청을 전달하고 캐시를 비운다`() = runTest {
        coEvery { cardService.deleteCard(1L) } returns ApiResponse(success = true, data = Unit)
        coEvery { cardLocalDataSource.deleteAll() } returns Unit

        assertEquals(AppResult.Success(Unit), repository.deleteCard(1L))
        coVerify(exactly = 1) { cardLocalDataSource.deleteAll() }
    }

    /** 지운 카드의 감정도 날짜도 모르므로, 그 한 장만 골라 지우는 대신 캐시 전체를 비운다. */
    @Test
    fun `이미 삭제된 카드도 성공으로 전달하며 캐시를 비운다`() = runTest {
        coEvery { cardService.deleteCard(1L) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "CARD_ALREADY_DELETED", message = "이미 삭제됨"),
        )
        coEvery { cardLocalDataSource.deleteAll() } returns Unit

        assertEquals(AppResult.Success(Unit), repository.deleteCard(1L))
        coVerify(exactly = 1) { cardLocalDataSource.deleteAll() }
    }

    @Test
    fun `카드 삭제 실패 envelope는 실패로 전달하고 캐시를 건드리지 않는다`() = runTest {
        coEvery { cardService.deleteCard(1L) } returns ApiResponse<Unit>(
            success = false,
            error = ApiError(code = "CARD_NOT_FOUND", message = "존재하지 않음"),
        )

        val result = repository.deleteCard(1L)

        assertEquals(
            "CARD_NOT_FOUND",
            ((result as AppResult.Failure).throwable as ApiException).code,
        )
        coVerify(exactly = 0) { cardLocalDataSource.deleteAll() }
    }

    @Test
    fun `감정별 카드 삭제 요청을 전달하고 캐시를 비운다`() = runTest {
        coEvery { cardService.deleteCardsByEmotion("ANGER") } returns ApiResponse(
            success = true,
            data = CardDeleteResponse(deletedCount = 0),
        )
        coEvery { cardLocalDataSource.deleteAll() } returns Unit

        assertEquals(AppResult.Success(Unit), repository.deleteCardsByEmotion(EmotionCharacter.ANGER))
        coVerify(exactly = 1) { cardLocalDataSource.deleteAll() }
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

    /** 새로 만든 카드가 그 날짜 캐시에 없는 채로 남으면, 그 날은 다시 열어도 계속 예전 목록만 보인다. */
    @Test
    fun `카드 생성에 성공하면 캐시를 비운다`() = runTest {
        coEvery { cardService.createCard(any()) } returns ApiResponse(
            success = true,
            data = cardResponse(id = 9L, emotion = "JOY"),
        )
        coEvery { cardLocalDataSource.deleteAll() } returns Unit

        val result = repository.createCard(
            conversationId = 10L,
            character = EmotionCharacter.JOY,
            summary = "요약",
        )

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { cardLocalDataSource.deleteAll() }
    }

    @Test
    fun `그 감정 칸의 그 달이 캐시에 있으면 서버를 호출하지 않는다`() = runTest {
        val cached = listOf(cardEntity(id = 1L), cardEntity(id = 2L, date = "2026-08-16"))
        coEvery {
            cardLocalDataSource.findByEmotionAndMonth("ANGER", YearMonth.of(2026, 8))
        } returns cached

        val result = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        assertEquals(cached.map { it.toDomain() }, (result as AppResult.Success).data)
        coVerify(exactly = 0) { cardService.getCardsByMonthAndEmotion(any(), any()) }
    }

    @Test
    fun `캐시가 비어 있으면 서버에서 받아 캐시에 저장한다`() = runTest {
        stubEmptyMonthCache()
        val upserted = slot<List<CardEntity>>()
        coEvery { cardLocalDataSource.upsertAll(capture(upserted)) } returns Unit
        coEvery { cardService.getCardsByMonthAndEmotion(any(), any()) } returns ApiResponse(
            success = true,
            data = listOf(cardResponse(id = 2L, emotion = "ANGER"), cardResponse(id = 1L, emotion = "ANGER")),
        )

        repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        assertEquals(listOf(2L, 1L), upserted.captured.map { it.id })
    }

    @Test
    fun `감정과 월을 API 형식으로 조회하고 오래된 순으로 돌려준다`() = runTest {
        stubEmptyMonthCache()
        coEvery { cardService.getCardsByMonthAndEmotion("ANGER", "2026-08") } returns ApiResponse(
            success = true,
            data = listOf(
                cardResponse(emotion = "ANGER").copy(id = 2L, date = "2026-08-16"),
                cardResponse(emotion = "ANGER").copy(id = 1L, date = "2026-08-15"),
            ),
        )

        val result = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        assertEquals(listOf(1L, 2L), (result as AppResult.Success).data.map { it.id })
        coVerify(exactly = 1) { cardService.getCardsByMonthAndEmotion("ANGER", "2026-08") }
    }

    @Test
    fun `캐릭터를 서버 감정 이름으로 바꿔 경로에 넣는다`() = runTest {
        stubEmptyMonthCache()
        coEvery { cardService.getCardsByMonthAndEmotion("GRUMPY", any()) } returns ApiResponse(
            success = true,
            data = emptyList(),
        )

        repository.getCardsByMonthAndEmotion(EmotionCharacter.PRICKLY, YearMonth.of(2026, 8))

        coVerify(exactly = 1) { cardService.getCardsByMonthAndEmotion("GRUMPY", "2026-08") }
    }

    @Test
    fun `못 읽는 카드는 그것만 버리고 나머지 달은 살린다`() = runTest {
        stubEmptyMonthCache()
        coEvery { cardService.getCardsByMonthAndEmotion(any(), any()) } returns ApiResponse(
            success = true,
            data = listOf(
                cardResponse(emotion = "ANGER").copy(id = 2L, date = "2026-08-99"),
                cardResponse(emotion = "ANGER").copy(id = 1L),
            ),
        )

        val result = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        assertEquals(listOf(1L), (result as AppResult.Success).data.map { it.id })
    }

    @Test
    fun `대상이 없는 달은 빈 목록으로 성공한다`() = runTest {
        stubEmptyMonthCache()
        coEvery { cardService.getCardsByMonthAndEmotion(any(), any()) } returns ApiResponse(
            success = true,
            data = emptyList(),
        )

        val result = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        assertTrue((result as AppResult.Success).data.isEmpty())
    }

    /**
     * 같은 목록인데 캐시에서 왔는지 서버에서 왔는지에 따라 순서가 달라지면 종이 더미가 진입 경로마다
     * 다르게 쌓인다. 두 경로가 같은 기준으로 정렬하는지 못 박는다.
     */
    @Test
    fun `캐시 경로와 서버 경로가 같은 순서를 돌려준다`() = runTest {
        stubEmptyMonthCache()
        coEvery { cardService.getCardsByMonthAndEmotion(any(), any()) } returns ApiResponse(
            success = true,
            data = listOf(
                cardResponse(emotion = "ANGER").copy(id = 3L, date = "2026-08-17"),
                cardResponse(emotion = "ANGER").copy(id = 2L, date = "2026-08-15"),
                cardResponse(emotion = "ANGER").copy(id = 1L, date = "2026-08-15"),
            ),
        )
        val fromServer = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        coEvery {
            cardLocalDataSource.findByEmotionAndMonth("ANGER", YearMonth.of(2026, 8))
        } returns listOf(
            cardEntity(id = 3L, date = "2026-08-17"),
            cardEntity(id = 2L),
            cardEntity(id = 1L),
        )
        val fromCache = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        val serverOrder = (fromServer as AppResult.Success).data.map { it.id }
        assertEquals(listOf(1L, 2L, 3L), serverOrder)
        assertEquals(serverOrder, (fromCache as AppResult.Success).data.map { it.id })
    }

    /** 폴백 경로가 경고를 남기므로 Log 를 세워 둔다. 유닛 테스트에서는 mock 이 없어 그대로 던진다. */
    @Test
    fun `캐시 조회가 실패하면 서버 조회로 대체한다`() = runTest {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        try {
            coEvery {
                cardLocalDataSource.findByEmotionAndMonth(any(), any())
            } throws IllegalStateException("db")
            coEvery { cardLocalDataSource.upsertAll(any()) } returns Unit
            coEvery { cardService.getCardsByMonthAndEmotion(any(), any()) } returns ApiResponse(
                success = true,
                data = listOf(cardResponse(emotion = "ANGER")),
            )

            val result = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

            assertEquals(listOf(1L), (result as AppResult.Success).data.map { it.id })
            coVerify(exactly = 1) { cardService.getCardsByMonthAndEmotion(any(), any()) }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    /** 빈 달은 `data = []` 로 온다는 계약에 기댄다. null 이면 빈 상태가 아니라 오류로 알린다. */
    @Test
    fun `월별 감정 응답에 카드 데이터가 없으면 실패로 전한다`() = runTest {
        stubEmptyMonthCache()
        coEvery { cardService.getCardsByMonthAndEmotion(any(), any()) } returns ApiResponse(success = true)

        val result = repository.getCardsByMonthAndEmotion(EmotionCharacter.ANGER, YearMonth.of(2026, 8))

        assertTrue((result as AppResult.Failure).throwable is IllegalStateException)
    }

    @Test
    fun `캐시를 비울 때 서버에는 아무 요청도 보내지 않는다`() = runTest {
        coEvery { cardLocalDataSource.deleteAll() } returns Unit

        repository.clearCache()

        coVerify(exactly = 1) { cardLocalDataSource.deleteAll() }
    }

    /**
     * 로그아웃 중 세션 정리를 무너뜨리거나, Orbit intent 안에서 전역 예외 핸들러 없이 그대로
     * 크래시로 번지지 않도록 호출부가 아니라 여기서 막는다.
     */
    @Test
    fun `캐시 삭제가 실패해도 던지지 않는다`() = runTest {
        coEvery { cardLocalDataSource.deleteAll() } throws IllegalStateException("disk error")

        repository.clearCache()
    }

    private fun stubEmptyMonthCache() {
        coEvery { cardLocalDataSource.findByEmotionAndMonth(any(), any()) } returns emptyList()
        coEvery { cardLocalDataSource.upsertAll(any()) } returns Unit
    }

    private fun cardEntity(id: Long, emotion: String = "ANGER", date: String = "2026-08-15") = CardEntity(
        id = id,
        conversationId = id,
        emotion = emotion,
        emotionLabel = "분노",
        summary = "요약",
        message = "대사",
        date = date,
    )

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 8, 15)

        fun cardResponse(id: Long = 1L, emotion: String) = CardResponse(
            id = id,
            conversationId = 10L,
            emotion = emotion,
            emotionLabel = "분노",
            summary = "회의가 길어졌다",
            message = "오늘 많이 힘들었겠다",
            date = "2026-08-15",
        )
    }
}
