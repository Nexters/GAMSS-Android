package com.gamss.android.feature.archive

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.GetCardsByMonthUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveDetailViewModelTest {

    @Test
    fun `선택한 감정의 카드만 종이 목록으로 남긴다`() = runTest {
        val viewModel = viewModel(AppResult.Success(listOf(angerEntry, joyEntry)))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState {
                copy(
                    emotion = EmotionCharacter.ANGER,
                    isLoading = false,
                    cards = listOf(angerEntry),
                )
            }
        }
    }

    @Test
    fun `카드 조회에 실패하면 오류 상태를 표시한다`() = runTest {
        val viewModel = viewModel(AppResult.Failure(IllegalStateException("network")))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState {
                copy(
                    emotion = EmotionCharacter.ANGER,
                    isLoading = false,
                    loadFailed = true,
                )
            }
        }
    }

    @Test
    fun `다른 달을 고르면 그 달을 다시 조회하고 시트를 닫는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerEntry)))
        val viewModel = ArchiveDetailViewModel(GetCardsByMonthUseCase(repository))
        val previousMonth = YearMonth.of(2026, 7)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(emotion = EmotionCharacter.ANGER, isLoading = false, cards = listOf(angerEntry)) }

            containerHost.showMonthPicker()
            expectState { copy(isMonthPickerVisible = true) }

            containerHost.selectMonth(previousMonth)
            expectState {
                copy(
                    yearMonth = previousMonth,
                    isMonthPickerVisible = false,
                    isLoading = true,
                    cards = emptyList(),
                )
            }
            expectState { copy(isLoading = false, cards = listOf(angerEntry)) }
        }

        assertEquals(previousMonth, repository.requestedMonths.last())
        assertEquals(2, repository.requestedMonths.size)
    }

    @Test
    fun `보고 있는 달을 다시 고르면 다시 조회하지 않고 시트만 닫는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerEntry)))
        val viewModel = ArchiveDetailViewModel(GetCardsByMonthUseCase(repository))
        val currentMonth = YearMonth.now(KoreanTimeZone)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(emotion = EmotionCharacter.ANGER, isLoading = false, cards = listOf(angerEntry)) }

            containerHost.showMonthPicker()
            expectState { copy(isMonthPickerVisible = true) }

            containerHost.selectMonth(currentMonth)
            expectState { copy(isMonthPickerVisible = false) }
        }

        assertEquals(listOf(currentMonth), repository.requestedMonths)
    }

    private fun viewModel(result: AppResult<List<CardEntry>>) = ArchiveDetailViewModel(
        GetCardsByMonthUseCase(FakeCardRepository(result)),
    )

    private companion object {
        val angerEntry = CardEntry(
            date = LocalDate.of(2026, 8, 15),
            indexInDate = 0,
            character = EmotionCharacter.ANGER,
        )
        val joyEntry = CardEntry(
            date = LocalDate.of(2026, 8, 15),
            indexInDate = 1,
            character = EmotionCharacter.JOY,
        )
    }
}

private class FakeCardRepository(
    private val entriesResult: AppResult<List<CardEntry>>,
) : CardRepository {

    val requestedMonths = mutableListOf<YearMonth>()

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
        AppResult.Success(emptyList())

    override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> {
        requestedMonths += yearMonth
        return entriesResult
    }

    override suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card> = error("보관함 테스트에서 쓰지 않는다")

    override suspend fun deleteCard(cardId: Long): AppResult<Unit> = error("보관함 테스트에서 쓰지 않는다")
}
