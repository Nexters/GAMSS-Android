package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.core.ui.share.StoryShareResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.GetCardsByMonthAndEmotionUseCase
import com.gamss.android.domain.card.MonthlyEmotionQuery
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
    private val getCardsByMonthAndEmotion: GetCardsByMonthAndEmotionUseCase,
    private val getConversation: GetConversationUseCase,
) : ViewModel(), ContainerHost<ArchiveDetailState, ArchiveDetailSideEffect> {

    override val container = container<ArchiveDetailState, ArchiveDetailSideEffect>(
        ArchiveDetailState(yearMonth = YearMonth.now(KoreanTimeZone)),
    )

    /**
     * 보던 달의 카드를 받아 온다.
     *
     * @param force 같은 감정이어도 다시 받는다. 방금 버린 카드를 보고 들어왔을 때 쓴다. 그 칸의
     *  ViewModel 이 back stack 에 살아남아 새 카드가 빠진 목록을 그대로 들고 있다.
     */
    fun load(emotion: EmotionCharacter, force: Boolean = false) = intent {
        if (!force && state.emotion == emotion) return@intent

        // 다시 받는 동안 지난 목록을 남겨 두면 방금 버린 카드가 없는 더미를 먼저 쏟는다. 그 사이
        // 낙하 신호가 소비돼, 목록이 도착해도 그 한 장만 떨어지는 연출이 나오지 않는다.
        reduce { state.copy(emotion = emotion, cards = ArchiveCards.Loading) }
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
        // 카드를 고르지 않았으니 이 감정 칸을 통째로 비운다. 다른 칸은 건드리지 않는다.
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

    fun selectCard(card: Card) = intent {
        reduce { state.copy(selectedCard = card) }
    }

    fun dismissCard() = intent {
        reduce { state.copy(selectedCard = null) }
    }

    /**
     * 한 장 버리기도 되돌릴 수 없어 파쇄 화면을 거친다. 지운 카드를 목록에서 빼는 일은 그 화면에서
     * 돌아올 때 [removeCard] 가 맡는다.
     */
    fun discardSelectedCard() = intent {
        val card = state.selectedCard ?: return@intent
        reduce { state.copy(selectedCard = null) }
        postSideEffect(ArchiveDetailSideEffect.OpenCardDelete(cardId = card.id))
    }

    /**
     * 파쇄 화면에서 지우고 온 카드를 목록에서 뺀다.
     *
     * 파쇄가 끝났다는 건 서버에서 이미 지워졌다는 뜻이고, 카드를 날짜·순번이 아니라 id 로 들고
     * 있으므로 그 한 장만 빼도 남은 목록이 서버와 어긋나지 않는다. 통째로 다시 받으면 남은 종이가
     * 사라졌다 다시 쌓이고, 조회가 실패하면 지운 카드와 상관없는 나머지까지 못 보게 된다.
     *
     * 목록을 아직 못 받았거나 그 사이 달을 옮겨 그 카드가 없으면 뺄 것이 없어 그대로 둔다.
     */
    fun removeCard(cardId: Long) = intent {
        reduce {
            when (val loaded = state.cards) {
                is ArchiveCards.Loaded ->
                    state.copy(cards = ArchiveCards.Loaded(loaded.cards.filterNot { it.id == cardId }))

                else -> state
            }
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

    fun showShareSheet() = intent {
        reduce { state.copy(isShareSheetVisible = true) }
    }

    fun dismissShareSheet() = intent {
        reduce { state.copy(isShareSheetVisible = false) }
    }

    /** [result] 는 항상 실패인 경우만 넘어온다. 호출부가 성공(Shared)까지 올리지 않는다. */
    fun reportInstagramShareFailure(result: StoryShareResult) = intent {
        postSideEffect(ArchiveDetailSideEffect.CardShareFailed(result))
    }

    fun reportKakaoTalkShareFailure() = intent {
        postSideEffect(ArchiveDetailSideEffect.KakaoTalkShareFailed)
    }

    private suspend fun Syntax<ArchiveDetailState, ArchiveDetailSideEffect>.loadMonth(
        emotion: EmotionCharacter,
        yearMonth: YearMonth,
    ) {
        val cards = when (val result = getCardsByMonthAndEmotion(MonthlyEmotionQuery(emotion, yearMonth))) {
            is AppResult.Success -> ArchiveCards.Loaded(result.data)
            is AppResult.Failure -> ArchiveCards.LoadFailed
        }
        // intent 는 서로 병렬로 돈다. 느린 달을 기다리는 사이 다른 달로 옮겼다면, 늦게 온 응답이
        // 지금 보고 있는 달의 목록을 덮어써 셀렉터와 종이가 어긋난다. 그래서 덮기 직전에 다시 확인한다.
        reduce {
            if (state.yearMonth == yearMonth && state.emotion == emotion) state.copy(cards = cards) else state
        }
    }
}
