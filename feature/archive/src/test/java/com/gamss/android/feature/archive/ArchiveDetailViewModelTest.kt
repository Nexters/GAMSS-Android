package com.gamss.android.feature.archive

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.ClearCardCacheUseCase
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

    /** 재조회까지 마쳤는데도 그 순번이 없다면 서버에도 정말 없는 카드다. */
    @Test
    fun `재조회해도 그 순번에 카드가 없으면 상세를 올리지 않고 실패를 알린다`() = runTest {
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

        assertEquals(1, repository.clearCacheCallCount)
        assertEquals(listOf(angerEntry.date, angerEntry.date), repository.requestedDates)
    }

    /**
     * 다른 기기에서 그 날짜에 카드가 추가되면 월별 응답엔 새 순번이 보이지만, 이 기기의 날짜 캐시는
     * 그 전 상태로 멈춰 있어 그 순번을 못 찾을 수 있다. 캐시를 비우고 한 번 더 받으면 찾아야 한다.
     */
    @Test
    fun `해당 순번에 카드가 없으면 캐시를 비우고 한 번 더 조회해 상세를 올린다`() = runTest {
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(listOf(angerEntry, joyEntry)),
            dateResult = AppResult.Success(listOf(firstCardOfDay)),
            dateResultAfterClear = AppResult.Success(listOf(firstCardOfDay, secondCardOfDay)),
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

        assertEquals(1, repository.clearCacheCallCount)
        assertEquals(listOf(joyEntry.date, joyEntry.date), repository.requestedDates)
    }

    /** 이미 네트워크까지 갔다가 실패한 경우엔 재시도로 캐시를 다시 비우지 않는다. */
    @Test
    fun `카드 조회 자체가 실패하면 재조회하지 않고 바로 실패를 알린다`() = runTest {
        val repository = FakeCardRepository(
            monthResult = AppResult.Success(listOf(angerEntry)),
            dateResult = AppResult.Failure(IllegalStateException("network")),
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

        assertEquals(0, repository.clearCacheCallCount)
        assertEquals(listOf(angerEntry.date), repository.requestedDates)
    }

    @Test
    fun `카드를 버리면 그 카드를 지우는 파쇄 화면을 연다`() = runTest {
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

            // 실제 삭제는 파쇄 화면이 맡으므로 여기서는 카드를 지우지 않는다.
            containerHost.discardSelectedCard()
            expectState { copy(selectedCard = null) }
            expectSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = firstCardOfDay.id))
        }

        // 다시 받아 오는 일은 파쇄 화면에서 돌아올 때 force load 가 맡는다.
        assertEquals(1, repository.requestedMonths.size)
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
            expectSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = null))
        }
    }

    private fun viewModel(repository: FakeCardRepository) = ArchiveDetailViewModel(
        getCardsByMonth = GetCardsByMonthUseCase(repository),
        getCardsByDate = GetCardsByDateUseCase(repository),
        clearCardCache = ClearCardCacheUseCase(repository),
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
    /** clearCache() 이후의 getCardsByDate 응답. 지정하지 않으면 [dateResult] 를 그대로 다시 준다. */
    private val dateResultAfterClear: AppResult<List<Card>>? = null,
    /** 여기 담긴 달은 게이트가 열릴 때까지 응답을 붙잡는다. 늦게 도착하는 응답을 만들 때 쓴다. */
    private val monthGates: Map<YearMonth, CompletableDeferred<Unit>> = emptyMap(),
    /** 달마다 다른 목록을 줘야 할 때만 채운다. 없는 달은 [monthResult] 로 답한다. */
    private val monthResults: Map<YearMonth, AppResult<List<CardEntry>>> = emptyMap(),
) : CardRepository {

    val requestedMonths = mutableListOf<YearMonth>()
    val requestedDates = mutableListOf<LocalDate>()
    var clearCacheCallCount = 0
        private set

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> {
        requestedDates += date
        return if (clearCacheCallCount > 0) dateResultAfterClear ?: dateResult else dateResult
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

    override suspend fun deleteCard(cardId: Long): AppResult<Unit> =
        error("실제 삭제는 파쇄 화면이 맡는다")

    override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
        error("보관함 테스트에서 쓰지 않는다")

    override suspend fun clearCache() {
        clearCacheCallCount++
    }
}
