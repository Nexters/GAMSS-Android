package com.gamss.android.feature.archive

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.DeleteCardUseCase
import com.gamss.android.domain.card.GetCardsByDateUseCase
import com.gamss.android.domain.card.GetCardsByMonthUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
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
        val viewModel = viewModel(FakeCardRepository(AppResult.Success(listOf(angerEntry, joyEntry))))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }
        }
    }

    @Test
    fun `카드 조회에 실패하면 오류 상태를 표시한다`() = runTest {
        val viewModel = viewModel(FakeCardRepository(AppResult.Failure(IllegalStateException("network"))))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.LoadFailed) }
        }
    }

    @Test
    fun `다른 달을 고르면 그 달을 다시 조회하고 시트를 닫는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerEntry)))
        val viewModel = viewModel(repository)
        val previousMonth = YearMonth.of(2026, 7)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.showMonthPicker()
            expectState { copy(isMonthPickerVisible = true) }

            containerHost.selectMonth(previousMonth)
            expectState {
                copy(
                    yearMonth = previousMonth,
                    isMonthPickerVisible = false,
                    cards = ArchiveCards.Loading,
                )
            }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }
        }

        assertEquals(previousMonth, repository.requestedMonths.last())
        assertEquals(2, repository.requestedMonths.size)
    }

    @Test
    fun `보고 있는 달을 다시 고르면 다시 조회하지 않고 시트만 닫는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerEntry)))
        val viewModel = viewModel(repository)
        val currentMonth = YearMonth.now(KoreanTimeZone)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.showMonthPicker()
            expectState { copy(isMonthPickerVisible = true) }

            containerHost.selectMonth(currentMonth)
            expectState { copy(isMonthPickerVisible = false) }
        }

        assertEquals(listOf(currentMonth), repository.requestedMonths)
    }

    @Test
    fun `종이를 누르면 그 날짜의 그날 순번 카드를 상세로 올린다`() = runTest {
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(listOf(angerEntry, joyEntry)),
            dateResult = AppResult.Success(listOf(firstCardOfDay, secondCardOfDay)),
        )
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.selectCard(joyEntry)
            expectState { copy(isCardLoading = true) }
            expectState { copy(isCardLoading = false, selectedCard = secondCardOfDay) }
        }

        assertEquals(listOf(joyEntry.date), repository.requestedDates)
    }

    @Test
    fun `그 순번에 카드가 없으면 상세를 올리지 않고 실패를 알린다`() = runTest {
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(listOf(angerEntry)),
            dateResult = AppResult.Success(emptyList()),
        )
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.selectCard(angerEntry)
            expectState { copy(isCardLoading = true) }
            expectState { copy(isCardLoading = false) }
            expectSideEffect(ArchiveDetailSideEffect.CardLoadFailed)
        }
    }

    @Test
    fun `카드를 버리면 삭제하고 그 달을 다시 조회한다`() = runTest {
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(listOf(angerEntry)),
            dateResult = AppResult.Success(listOf(firstCardOfDay)),
        )
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.selectCard(angerEntry)
            expectState { copy(isCardLoading = true) }
            expectState { copy(isCardLoading = false, selectedCard = firstCardOfDay) }

            containerHost.discardSelectedCard()
            expectState { copy(selectedCard = null) }
        }

        // 재조회 결과가 이전과 같은 상태라 emission 이 더 없다. 다시 받아 왔는지는 호출로 확인한다.
        assertEquals(listOf(firstCardOfDay.id), repository.deletedCardIds)
        assertEquals(2, repository.requestedMonths.size)
    }

    @Test
    fun `대화보기를 누르면 그 카드의 대화방을 연다`() = runTest {
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(listOf(angerEntry)),
            dateResult = AppResult.Success(listOf(firstCardOfDay)),
        )
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.selectCard(angerEntry)
            expectState { copy(isCardLoading = true) }
            expectState { copy(isCardLoading = false, selectedCard = firstCardOfDay) }

            containerHost.viewSelectedConversation()
            expectState { copy(selectedCard = null) }
            expectSideEffect(ArchiveDetailSideEffect.OpenChatRoom(firstCardOfDay.conversationId))
        }
    }

    @Test
    fun `늦게 온 이전 달 응답은 지금 보고 있는 달을 덮지 않는다`() = runTest {
        val currentMonth = YearMonth.now(KoreanTimeZone)
        val slowMonth = currentMonth.minusMonths(2)
        val fastMonth = currentMonth.minusMonths(1)
        val slowMonthGate = CompletableDeferred<Unit>()
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(emptyList()),
            monthGates = mapOf(slowMonth to slowMonthGate),
            monthResults = mapOf(
                slowMonth to AppResult.Success(listOf(staleEntry)),
                fastMonth to AppResult.Success(listOf(angerEntry)),
            ),
        )
        val viewModel = viewModel(repository)
        val testScope = this

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(emptyList())) }

            // 응답이 게이트에 걸려 멈춰 있는 동안 다음 달을 고른다.
            containerHost.selectMonth(slowMonth)
            expectState { copy(yearMonth = slowMonth, cards = ArchiveCards.Loading) }

            containerHost.selectMonth(fastMonth)
            expectState { copy(yearMonth = fastMonth, cards = ArchiveCards.Loading) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            // 뒤늦게 도착한 이전 달 응답이 지금 목록을 덮으면 소비되지 않은 상태가 남는다.
            slowMonthGate.complete(Unit)
            testScope.runCurrent()
            expectNoItems()
        }
    }

    @Test
    fun `비우기를 확인하면 다이얼로그를 닫고 파쇄 화면을 연다`() = runTest {
        val viewModel = viewModel(FakeCardRepository(AppResult.Success(listOf(angerEntry))))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerEntry))) }

            containerHost.showClearDialog()
            expectState { copy(isClearDialogVisible = true) }

            // 실제 삭제는 파쇄 화면이 맡으므로 여기서는 카드를 지우지 않는다.
            containerHost.confirmClear()
            expectState { copy(isClearDialogVisible = false) }
            expectSideEffect(ArchiveDetailSideEffect.OpenCardDelete)
        }
    }

    private fun viewModel(repository: FakeCardRepository) = ArchiveDetailViewModel(
        getCardsByMonth = GetCardsByMonthUseCase(repository),
        getCardsByDate = GetCardsByDateUseCase(repository),
        deleteCard = DeleteCardUseCase(repository),
    )

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 8, 15)

        val angerEntry = CardEntry(date = DATE, indexInDate = 0, character = EmotionCharacter.ANGER)
        val staleEntry = CardEntry(date = DATE.minusMonths(2), indexInDate = 0, character = EmotionCharacter.ANGER)
        val joyEntry = CardEntry(date = DATE, indexInDate = 1, character = EmotionCharacter.JOY)

        val firstCardOfDay = card(id = 1L, character = EmotionCharacter.ANGER)
        val secondCardOfDay = card(id = 2L, character = EmotionCharacter.JOY)

        fun card(id: Long, character: EmotionCharacter) = Card(
            id = id,
            conversationId = id * 10,
            character = character,
            emotionLabel = character.displayName,
            summary = "요약 $id",
            message = "대사 $id",
            date = DATE,
        )
    }
}

private class FakeCardRepository(
    private val monthResult: AppResult<List<CardEntry>>,
    private val dateResult: AppResult<List<Card>> = AppResult.Success(emptyList()),
    /** 여기 담긴 달은 게이트가 열릴 때까지 응답을 붙잡는다. 늦게 도착하는 응답을 만들 때 쓴다. */
    private val monthGates: Map<YearMonth, CompletableDeferred<Unit>> = emptyMap(),
    /** 달마다 다른 목록을 줘야 할 때만 채운다. 없는 달은 [monthResult] 로 답한다. */
    private val monthResults: Map<YearMonth, AppResult<List<CardEntry>>> = emptyMap(),
) : CardRepository {

    val requestedMonths = mutableListOf<YearMonth>()
    val requestedDates = mutableListOf<LocalDate>()
    val deletedCardIds = mutableListOf<Long>()

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> {
        requestedDates += date
        return dateResult
    }

    override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> {
        requestedMonths += yearMonth
        monthGates[yearMonth]?.await()
        return monthResults[yearMonth] ?: monthResult
    }

    override suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card> = error("보관함 테스트에서 쓰지 않는다")

    override suspend fun deleteAllCards(): AppResult<Unit> =
        error("보관함 테스트에서 쓰지 않는다")

    override suspend fun deleteCard(cardId: Long): AppResult<Unit> {
        deletedCardIds += cardId
        return AppResult.Success(Unit)
    }

    override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
        error("보관함 테스트에서 쓰지 않는다")

    override suspend fun clearCache(): Unit = error("보관함 테스트에서 쓰지 않는다")
}
