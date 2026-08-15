package com.gamss.android.feature.calendar

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.GetCardsByDateUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @Test
    fun `selecting a date loads its cards`() = runTest {
        val selectedDate = LocalDate.of(2026, 8, 15)
        val expectedCard = card(date = selectedDate)
        val repository = RecordingCardRepository(expectedCard)

        CalendarViewModel(GetCardsByDateUseCase(repository)).test(this) {
            containerHost.selectDate(selectedDate)
            runCurrent()

            val state = containerHost.container.stateFlow.value
            assertEquals(selectedDate, repository.requestedDate)
            assertEquals(selectedDate, state.selectedDate)
            assertEquals(CalendarCardLoadState.Content(listOf(expectedCard)), state.cardLoadState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a failed request shows an error and can be retried`() = runTest {
        val selectedDate = LocalDate.of(2026, 8, 15)
        val repository = RecordingCardRepository(card(selectedDate), shouldFail = true)

        CalendarViewModel(GetCardsByDateUseCase(repository)).test(this) {
            containerHost.selectDate(selectedDate)
            runCurrent()
            assertEquals(CalendarCardLoadState.Error, containerHost.container.stateFlow.value.cardLoadState)

            repository.shouldFail = false
            containerHost.retrySelectedDate()
            runCurrent()

            assertEquals(2, repository.requestCount)
            assertEquals(
                CalendarCardLoadState.Content(listOf(card(selectedDate))),
                containerHost.container.stateFlow.value.cardLoadState,
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    private class RecordingCardRepository(
        private val card: Card,
        var shouldFail: Boolean = false,
    ) : CardRepository {
        var requestedDate: LocalDate? = null
            private set
        var requestCount: Int = 0
            private set

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> {
            requestedDate = date
            requestCount++
            return if (shouldFail) AppResult.Failure(IllegalStateException("Network failure")) else AppResult.Success(listOf(card))
        }

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = error("Not used by the calendar")
    }

    private fun card(date: LocalDate) = Card(
        id = 1L,
        conversationId = 2L,
        character = EmotionCharacter.ANGER,
        emotionLabel = "분노",
        summary = "비 때문에 하루가 꼬였어요",
        message = "비 때문에 하루가 꼬였어요",
        date = date,
    )
}
