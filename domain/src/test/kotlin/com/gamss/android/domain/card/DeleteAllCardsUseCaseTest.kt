package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DeleteAllCardsUseCaseTest {

    private class RecordingRepository : CardRepository {
        var deleteAllCalled = false

        override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> =
            error("카드 전체 삭제 테스트에서 쓰지 않는다")

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> = error("사용하지 않음")

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = error("사용하지 않음")

        override suspend fun deleteAllCards(): AppResult<Unit> {
            deleteAllCalled = true
            return AppResult.Success(Unit)
        }

        override suspend fun deleteCard(cardId: Long): AppResult<Unit> = error("사용하지 않음")

        override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
            error("사용하지 않음")
    }

    @Test
    fun 모든_카드_삭제를_리포지토리에_위임한다() = runBlocking {
        val repository = RecordingRepository()

        val result = DeleteAllCardsUseCase(repository)()

        assertEquals(true, repository.deleteAllCalled)
        assertEquals(AppResult.Success(Unit), result)
    }
}
