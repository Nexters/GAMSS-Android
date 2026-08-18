package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.GetCardsByDateUseCase
import com.gamss.android.domain.card.GetCardsByMonthUseCase
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
        postSideEffect(ArchiveDetailSideEffect.OpenCardShred(cardId = null))
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
        val card = when (val result = getCardsByDate(entry.date)) {
            is AppResult.Success -> result.data.getOrNull(entry.indexInDate)
            is AppResult.Failure -> null
        }
        reduce { state.copy(isCardLoading = false, selectedCard = card) }

        if (card == null) postSideEffect(ArchiveDetailSideEffect.CardLoadFailed)
    }

    fun dismissCard() = intent {
        reduce { state.copy(selectedCard = null) }
    }

    /**
     * 여기서 지우지 않는다. 파쇄 화면이 삭제까지 끝내고 보관함 목록으로 되돌려 보내므로, 이 화면은
     * 다시 들어올 때 새로 조회한다 — 남은 종이의 그날 순번이 당겨지는 것도 그때 함께 맞춰진다.
     */
    fun discardSelectedCard() = intent {
        val cardId = state.selectedCard?.id ?: return@intent
        reduce { state.copy(selectedCard = null) }
        postSideEffect(ArchiveDetailSideEffect.OpenCardShred(cardId))
    }

    fun viewSelectedConversation() = intent {
        val conversationId = state.selectedCard?.conversationId ?: return@intent
        reduce { state.copy(selectedCard = null) }
        postSideEffect(ArchiveDetailSideEffect.OpenChatRoom(conversationId))
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
