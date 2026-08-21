package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.ClearCardCacheUseCase
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
    private val clearCardCache: ClearCardCacheUseCase,
    private val getConversation: GetConversationUseCase,
) : ViewModel(), ContainerHost<ArchiveDetailState, ArchiveDetailSideEffect> {

    override val container = container<ArchiveDetailState, ArchiveDetailSideEffect>(
        ArchiveDetailState(yearMonth = YearMonth.now(KoreanTimeZone)),
    )

    /**
     * 보던 달의 카드를 받아 온다.
     *
     * @param force 같은 감정이어도 다시 받는다. 방금 버린 카드를 보고 들어왔을 때 쓴다. 이 칸에
     *  이미 들어와 있던 채로 또 버리면 back stack 이 그대로라 ViewModel 도 살아남는데, 그때
     *  건너뛰면 방금 만든 카드가 목록에 안 들어온다.
     */
    fun load(emotion: EmotionCharacter, force: Boolean = false) = intent {
        if (!force && state.emotion == emotion) return@intent

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
        postSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = null))
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

    /**
     * 한 장 버리기도 되돌릴 수 없어 파쇄 화면을 거친다. 지운 뒤 목록을 다시 받는 일은 그 화면에서
     * 돌아올 때 [load] 가 맡는다 — 카드를 지우면 같은 날짜 뒤 순번이 한 칸씩 당겨지므로, 목록에서
     * 빼는 것만으로는 남은 종이의 순번이 서버와 어긋난다.
     */
    fun discardSelectedCard() = intent {
        val card = state.selectedCard ?: return@intent
        reduce { state.copy(selectedCard = null) }
        postSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = card.id))
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
        reduce {
            // 기다리는 사이 사용자가 카드를 닫거나 다른 카드를 열었으면 이 응답은 지난 요청의 것이다.
            // 성공이든 실패든 반영하지 않는다.
            val isStale = state.selectedCard?.id != card.id
            if (isStale || conversationCard == null) {
                state.copy(isConversationLoading = false)
            } else {
                state.copy(
                    isConversationLoading = false,
                    selectedCard = null,
                    conversationCard = conversationCard,
                )
            }
        }

        // 닫아 버린 카드의 실패를 뒤늦게 알리지 않는다.
        if (conversationCard == null && state.selectedCard?.id == card.id) {
            postSideEffect(ArchiveDetailSideEffect.ConversationLoadFailed)
        }
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
