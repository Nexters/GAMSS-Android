package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.DeleteCardUseCase
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
    private val deleteCard: DeleteCardUseCase,
) : ViewModel(), ContainerHost<ArchiveDetailState, ArchiveDetailSideEffect> {

    override val container = container<ArchiveDetailState, ArchiveDetailSideEffect>(
        ArchiveDetailState(yearMonth = YearMonth.now(KoreanTimeZone)),
    )

    fun load(emotion: EmotionCharacter) = intent {
        if (state.emotion == emotion) return@intent

        reduce { state.copy(emotion = emotion) }
        loadMonth(emotion, state.yearMonth)
    }

    /**
     * 방금 버린 카드를 보고 들어왔을 때 쓴다. 보던 달을 다시 받는다.
     *
     * [load] 는 같은 감정이면 건너뛴다. 그런데 이 칸에 이미 들어와 있던 채로 또 버리면 back stack
     * 이 그대로라 ViewModel 도 살아남아, 조기 반환에 걸려 방금 만든 카드가 목록에 안 들어온다.
     */
    fun reloadMonth(emotion: EmotionCharacter) = intent {
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
