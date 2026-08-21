package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.ClearCardCacheUseCase
import com.gamss.android.domain.card.DeleteCardUseCase
import com.gamss.android.domain.card.GetCardsByDateUseCase
import com.gamss.android.domain.card.GetCardsByMonthUseCase
import com.gamss.android.domain.conversation.GetConversationUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ArchiveDetailViewModel @Inject constructor(
    private val getCardsByMonth: GetCardsByMonthUseCase,
    private val getCardsByDate: GetCardsByDateUseCase,
    private val deleteCard: DeleteCardUseCase,
    private val clearCardCache: ClearCardCacheUseCase,
    private val getConversation: GetConversationUseCase,
) : ViewModel(), ContainerHost<ArchiveDetailState, ArchiveDetailSideEffect> {

    override val container = container<ArchiveDetailState, ArchiveDetailSideEffect>(
        ArchiveDetailState(yearMonth = YearMonth.now(KoreanTimeZone)),
    )

    fun load(emotion: EmotionCharacter) = intent {
        if (state.emotion == emotion) return@intent

        reduce { state.copy(emotion = emotion) }
        loadMonth(emotion, state.yearMonth)
    }

    fun showMonthPicker() = intent {
        reduce { state.copy(isMonthPickerVisible = true) }
    }

    fun dismissMonthPicker() = intent {
        reduce { state.copy(isMonthPickerVisible = false) }
    }

    fun showClearDialog() = intent {
        reduce { state.copy(isClearDialogVisible = true) }
    }

    fun dismissClearDialog() = intent {
        reduce { state.copy(isClearDialogVisible = false) }
    }

    fun confirmClear() = intent {
        reduce { state.copy(isClearDialogVisible = false) }
        postSideEffect(ArchiveDetailSideEffect.OpenCardDelete)
    }

    fun selectMonth(yearMonth: YearMonth) = intent {
        val emotion = state.emotion
        // 보고 있는 달을 다시 고르면 이미 쌓인 종이를 다시 쏟지 않고 시트만 닫는다.
        if (emotion == null || yearMonth == state.yearMonth) {
            reduce { state.copy(isMonthPickerVisible = false) }
            return@intent
        }

        reduce {
            state.copy(
                yearMonth = yearMonth,
                isMonthPickerVisible = false,
                cards = ArchiveCards.Loading,
            )
        }
        loadMonth(emotion, yearMonth)
    }

    /**
     * 월별 응답에는 카드 식별자가 없어 종이는 날짜와 그날 순번만 들고 있다. 눌린 종이의 날짜로 다시
     * 조회해 그 순번의 카드를 집어야 요약·대사와 id 가 손에 들어온다.
     */
    fun selectCard(entry: CardEntry) = intent {
        if (state.isCardLoading) return@intent

        reduce { state.copy(isCardLoading = true) }

        val firstResult = getCardsByDate(entry.date)
        val card = when (firstResult) {
            is AppResult.Success -> firstResult.data.getOrNull(entry.indexInDate)
                ?: run {
                    clearCardCache()
                    (getCardsByDate(entry.date) as? AppResult.Success)?.data?.getOrNull(entry.indexInDate)
                }
            is AppResult.Failure -> null
        }
        reduce { state.copy(isCardLoading = false, selectedCard = card) }

        if (card == null) postSideEffect(ArchiveDetailSideEffect.CardLoadFailed)
    }

    fun dismissCard() = intent {
        reduce { state.copy(selectedCard = null) }
    }

    fun discardSelectedCard() = intent {
        val card = state.selectedCard ?: return@intent
        val emotion = state.emotion ?: return@intent
        reduce { state.copy(selectedCard = null) }

        when (deleteCard(card.id)) {
            // 카드를 지우면 같은 날짜 뒤 순번이 한 칸씩 당겨진다. 목록에서 빼는 것으로는 남은 종이의
            // 순번이 어긋나므로 그 달을 다시 받아 온다.
            is AppResult.Success -> loadMonth(emotion, state.yearMonth)
            is AppResult.Failure -> postSideEffect(ArchiveDetailSideEffect.CardDiscardFailed)
        }
    }

    /**
     * 대화보기는 채팅방으로 나가지 않는다. 이미 종료된 대화라 이어 쓸 수 없어, 같은 자리에서 카드를
     * 뒤집어 그 대화 기록만 보여 준다.
     */
    fun viewSelectedConversation() = intent {
        val card = state.selectedCard ?: return@intent
        if (state.isConversationLoading) return@intent

        reduce { state.copy(isConversationLoading = true) }
        val conversationCard = when (val result = getConversation(card.conversationId)) {
            is AppResult.Success -> ConversationCard(card = card, messages = result.data.messages)
            is AppResult.Failure -> null
        }
        // 못 불러왔으면 감정 카드를 건드리지 않는다. 기다리는 사이 사용자가 카드를 닫았을 수도 있어
        // 진입 시점에 잡아 둔 card 로 되살리지 않고 지금 상태를 그대로 둔다.
        reduce {
            state.copy(
                isConversationLoading = false,
                selectedCard = if (conversationCard == null) state.selectedCard else null,
                conversationCard = conversationCard,
            )
        }

        if (conversationCard == null) postSideEffect(ArchiveDetailSideEffect.ConversationLoadFailed)
    }

    fun dismissConversationCard() = intent {
        val card = state.conversationCard?.card ?: return@intent
        reduce { state.copy(conversationCard = null, selectedCard = card) }
    }

    /** 월별 응답은 모든 감정을 섞어 주므로 이 화면이 보고 있는 감정만 남긴다. */
    private suspend fun Syntax<ArchiveDetailState, ArchiveDetailSideEffect>.loadMonth(
        emotion: EmotionCharacter,
        yearMonth: YearMonth,
    ) {
        val cards = when (val result = getCardsByMonth(yearMonth)) {
            is AppResult.Success -> ArchiveCards.Loaded(result.data.filter { it.character == emotion })
            is AppResult.Failure -> ArchiveCards.LoadFailed
        }
        // intent 는 서로 병렬로 돈다. 느린 달을 기다리는 사이 다른 달로 옮겼다면, 늦게 온 응답이
        // 지금 보고 있는 달의 목록을 덮어써 셀렉터와 종이가 어긋난다. 그래서 덮기 직전에 다시 확인한다.
        reduce {
            if (state.yearMonth == yearMonth && state.emotion == emotion) state.copy(cards = cards) else state
        }
    }
}
