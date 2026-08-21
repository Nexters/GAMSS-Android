package com.gamss.android.feature.archive

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.GetCardsByMonthAndEmotionUseCase
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationDetail
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.GetConversationUseCase
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    fun `보고 있는 감정과 달로 조회해 받은 목록을 그대로 쓴다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard, joyCard)))
        val viewModel = viewModel(repository)
        val currentMonth = YearMonth.now(KoreanTimeZone)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            // 감정 필터는 서버가 맡으므로 받은 목록을 클라이언트에서 다시 걸러내지 않는다.
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard, joyCard))) }
        }

        assertEquals(listOf(EmotionCharacter.ANGER to currentMonth), repository.requests)
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
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)
        val previousMonth = YearMonth.of(2026, 7)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

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
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }
        }

        assertEquals(EmotionCharacter.ANGER to previousMonth, repository.requests.last())
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `보고 있는 달을 다시 고르면 다시 조회하지 않고 시트만 닫는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)
        val currentMonth = YearMonth.now(KoreanTimeZone)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.showMonthPicker()
            expectState { copy(isMonthPickerVisible = true) }

            containerHost.selectMonth(currentMonth)
            expectState { copy(isMonthPickerVisible = false) }
        }

        assertEquals(listOf(EmotionCharacter.ANGER to currentMonth), repository.requests)
    }

    @Test
    fun `같은 감정으로 다시 들어오면 이미 받은 목록을 그대로 쓴다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.load(EmotionCharacter.ANGER)
        }

        assertEquals(1, repository.requests.size)
    }

    /**
     * 방금 버린 카드를 보고 들어오는 길이 이 스위치에 걸려 있다. 건너뛰면 그 카드가 빠진 지난
     * 목록이 그대로 남아 낙하할 종이를 못 찾는다.
     */
    @Test
    fun `force 면 같은 감정이어도 목록을 비우고 다시 받는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.load(EmotionCharacter.ANGER, force = true)
            // 다시 받는 동안 지난 더미를 먼저 쏟아 낙하 신호를 헛되게 쓰지 않도록 목록을 비운다.
            expectState { copy(cards = ArchiveCards.Loading) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }
        }

        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `종이를 누르면 조회 없이 그 카드를 상세로 올린다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard, joyCard)))
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard, joyCard))) }

            containerHost.selectCard(joyCard)
            expectState { copy(selectedCard = joyCard) }
        }

        // 목록이 이미 카드 내용까지 들고 있어 상세를 열 때 두 번째 조회가 없다.
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `카드를 버리면 그 카드를 지우는 파쇄 화면을 연다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.selectCard(angerCard)
            expectState { copy(selectedCard = angerCard) }

            // 실제 삭제는 파쇄 화면이 맡으므로 여기서는 카드를 지우지 않는다.
            containerHost.discardSelectedCard()
            expectState { copy(selectedCard = null) }
            expectSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = angerCard.id))
        }

        // 목록에서 빼는 일은 파쇄 화면에서 돌아올 때 removeCard 가 맡는다.
        assertEquals(1, repository.requests.size)
    }

    /** 파쇄 화면에서 돌아오는 길이 여기에 걸려 있다. 건너뛰면 지운 카드가 목록에 그대로 남는다. */
    @Test
    fun `파쇄하고 돌아오면 목록을 다시 받지 않고 그 카드만 뺀다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard, joyCard)))
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard, joyCard))) }

            containerHost.removeCard(angerCard.id)
            expectState { copy(cards = ArchiveCards.Loaded(listOf(joyCard))) }
        }

        // 남은 종이가 사라졌다 다시 쌓이지 않고, 조회가 실패해 나머지까지 못 보게 되는 일도 없다.
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `마지막 한 장을 파쇄하면 목록이 빈다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.removeCard(angerCard.id)
            expectState { copy(cards = ArchiveCards.Loaded(emptyList())) }
        }
    }

    /** 파쇄하고 돌아오는 사이 달을 옮겼으면 지운 카드가 지금 목록에 없다. 그때는 손대지 않는다. */
    @Test
    fun `목록에 없는 카드를 빼도 보고 있는 목록은 그대로다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(repository)
        val testScope = this

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.removeCard(staleCard.id)
            testScope.runCurrent()
            expectNoItems()
        }

        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `대화보기를 누르면 채팅방으로 나가지 않고 그 대화를 카드로 띄운다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val conversationRepository = FakeConversationRepository(
            AppResult.Success(conversationDetail(angerCard.conversationId)),
        )
        val viewModel = viewModel(repository, conversationRepository)

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.selectCard(angerCard)
            expectState { copy(selectedCard = angerCard) }

            containerHost.viewSelectedConversation()
            expectState { copy(isConversationLoading = true) }
            expectState {
                copy(
                    isConversationLoading = false,
                    selectedCard = null,
                    conversationCard = ConversationCard(
                        card = angerCard,
                        messages = listOf(userMessage, characterMessage),
                    ),
                )
            }
        }

        assertEquals(listOf(angerCard.conversationId), conversationRepository.requestedConversationIds)
    }

    @Test
    fun `대화 카드를 닫으면 원래 보던 감정 카드로 돌아온다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(
            repository,
            FakeConversationRepository(AppResult.Success(conversationDetail(angerCard.conversationId))),
        )

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.selectCard(angerCard)
            expectState { copy(selectedCard = angerCard) }

            containerHost.viewSelectedConversation()
            expectState { copy(isConversationLoading = true) }
            expectState {
                copy(
                    isConversationLoading = false,
                    selectedCard = null,
                    conversationCard = ConversationCard(
                        card = angerCard,
                        messages = listOf(userMessage, characterMessage),
                    ),
                )
            }

            containerHost.dismissConversationCard()
            expectState { copy(conversationCard = null, selectedCard = angerCard) }
        }
    }

    @Test
    fun `대화 조회에 실패하면 감정 카드를 그대로 두고 실패를 알린다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val viewModel = viewModel(
            repository,
            FakeConversationRepository(AppResult.Failure(IllegalStateException("network"))),
        )

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.selectCard(angerCard)
            expectState { copy(selectedCard = angerCard) }

            containerHost.viewSelectedConversation()
            expectState { copy(isConversationLoading = true) }
            expectState { copy(isConversationLoading = false) }
            expectSideEffect(ArchiveDetailSideEffect.ConversationLoadFailed)
        }
    }

    @Test
    fun `늦게 온 대화 실패 응답은 이미 닫은 감정 카드를 되살리지 않는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val conversationGate = CompletableDeferred<Unit>()
        val viewModel = viewModel(
            repository,
            FakeConversationRepository(AppResult.Failure(IllegalStateException("network")), conversationGate),
        )
        val testScope = this

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.selectCard(angerCard)
            expectState { copy(selectedCard = angerCard) }

            // 대화 응답이 게이트에 걸려 멈춰 있는 동안 감정 카드를 닫는다.
            containerHost.viewSelectedConversation()
            expectState { copy(isConversationLoading = true) }

            containerHost.dismissCard()
            expectState { copy(selectedCard = null) }

            // 닫아 버린 카드의 실패를 뒤늦게 토스트로 알리지 않는다.
            conversationGate.complete(Unit)
            testScope.runCurrent()
            expectState { copy(isConversationLoading = false) }
            expectNoItems()
        }
    }

    @Test
    fun `늦게 온 대화 성공 응답은 이미 닫은 감정 카드를 다시 열지 않는다`() = runTest {
        val repository = FakeCardRepository(AppResult.Success(listOf(angerCard)))
        val conversationGate = CompletableDeferred<Unit>()
        val viewModel = viewModel(
            repository,
            FakeConversationRepository(
                AppResult.Success(conversationDetail(angerCard.conversationId)),
                conversationGate,
            ),
        )
        val testScope = this

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.selectCard(angerCard)
            expectState { copy(selectedCard = angerCard) }

            containerHost.viewSelectedConversation()
            expectState { copy(isConversationLoading = true) }

            containerHost.dismissCard()
            expectState { copy(selectedCard = null) }

            // 요청을 시작한 카드가 이미 닫혔으므로 대화 카드로 전환하지 않는다.
            conversationGate.complete(Unit)
            testScope.runCurrent()
            expectState { copy(isConversationLoading = false) }
            expectNoItems()
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
                slowMonth to AppResult.Success(listOf(staleCard)),
                fastMonth to AppResult.Success(listOf(angerCard)),
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
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            // 뒤늦게 도착한 이전 달 응답이 지금 목록을 덮으면 소비되지 않은 상태가 남는다.
            slowMonthGate.complete(Unit)
            testScope.runCurrent()
            expectNoItems()
        }
    }

    @Test
    fun `비우기를 확인하면 다이얼로그를 닫고 파쇄 화면을 연다`() = runTest {
        val viewModel = viewModel(FakeCardRepository(AppResult.Success(listOf(angerCard))))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)
            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState { copy(cards = ArchiveCards.Loaded(listOf(angerCard))) }

            containerHost.showClearDialog()
            expectState { copy(isClearDialogVisible = true) }

            // 실제 삭제는 파쇄 화면이 맡으므로 여기서는 카드를 지우지 않는다.
            containerHost.confirmClear()
            expectState { copy(isClearDialogVisible = false) }
            expectSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = null))
        }
    }

    private fun viewModel(
        repository: FakeCardRepository,
        conversationRepository: FakeConversationRepository = FakeConversationRepository(),
    ) = ArchiveDetailViewModel(
        getCardsByMonthAndEmotion = GetCardsByMonthAndEmotionUseCase(repository),
        getConversation = GetConversationUseCase(conversationRepository),
    )

    private companion object {
        val DATE: LocalDate = LocalDate.of(2026, 8, 15)

        val angerCard = card(id = 1L, character = EmotionCharacter.ANGER)
        val joyCard = card(id = 2L, character = EmotionCharacter.JOY)
        val staleCard = card(id = 3L, character = EmotionCharacter.ANGER, date = DATE.minusMonths(2))

        fun card(id: Long, character: EmotionCharacter, date: LocalDate = DATE) = Card(
            id = id,
            conversationId = id * 10,
            character = character,
            emotionLabel = character.displayName,
            summary = "요약 $id",
            message = "대사 $id",
            date = date,
        )

        val userMessage = Message(
            id = 1L,
            conversationId = angerCard.conversationId,
            sender = MessageSender.User,
            content = "오늘 진짜 화났어",
            createdTime = "오후 1:37",
        )
        val characterMessage = Message(
            id = 2L,
            conversationId = angerCard.conversationId,
            sender = MessageSender.Character(EmotionCharacter.ANGER),
            content = "그럴 만했네!",
            createdTime = "오후 1:38",
        )

        fun conversationDetail(conversationId: Long) = ConversationDetail(
            conversation = Conversation(id = conversationId, title = "화났던 날"),
            messages = listOf(userMessage, characterMessage),
        )
    }
}

private class FakeCardRepository(
    private val monthResult: AppResult<List<Card>>,
    /** 여기 담긴 달은 게이트가 열릴 때까지 응답을 붙잡는다. 늦게 도착하는 응답을 만들 때 쓴다. */
    private val monthGates: Map<YearMonth, CompletableDeferred<Unit>> = emptyMap(),
    /** 달마다 다른 목록을 줘야 할 때만 채운다. 없는 달은 [monthResult] 로 답한다. */
    private val monthResults: Map<YearMonth, AppResult<List<Card>>> = emptyMap(),
) : CardRepository {

    /** 감정도 서버로 넘기므로 달만이 아니라 (감정, 달) 쌍으로 기록한다. */
    val requests = mutableListOf<Pair<EmotionCharacter, YearMonth>>()

    override suspend fun getCardsByMonthAndEmotion(
        character: EmotionCharacter,
        yearMonth: YearMonth,
    ): AppResult<List<Card>> {
        requests += character to yearMonth
        monthGates[yearMonth]?.await()
        return monthResults[yearMonth] ?: monthResult
    }

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
        error("보관함 테스트에서 쓰지 않는다")

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

    override suspend fun clearCache() = error("보관함 테스트에서 쓰지 않는다")
}

private class FakeConversationRepository(
    private val conversationResult: AppResult<ConversationDetail> =
        AppResult.Failure(IllegalStateException("대화를 준비하지 않았다")),
    /** 넘기면 게이트가 열릴 때까지 응답을 붙잡는다. 늦게 도착하는 응답을 만들 때 쓴다. */
    private val gate: CompletableDeferred<Unit>? = null,
) : ConversationRepository {

    val requestedConversationIds = mutableListOf<Long>()

    override suspend fun getConversation(conversationId: Long): AppResult<ConversationDetail> {
        requestedConversationIds += conversationId
        gate?.await()
        return conversationResult
    }

    override suspend fun getOngoingConversations(): AppResult<List<Conversation>> =
        error("보관함 테스트에서 쓰지 않는다")

    override suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
        excludeCharacters: Set<EmotionCharacter>,
    ): AppResult<SentMessage> = error("보관함 테스트에서 쓰지 않는다")

    override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> =
        error("보관함 테스트에서 쓰지 않는다")

    override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
        error("보관함 테스트에서 쓰지 않는다")

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
        error("보관함 테스트에서 쓰지 않는다")

    override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
        error("보관함 테스트에서 쓰지 않는다")
}
