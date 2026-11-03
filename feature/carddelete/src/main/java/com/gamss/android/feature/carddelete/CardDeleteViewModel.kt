package com.gamss.android.feature.carddelete

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.analytics.AnalyticsEvent
import com.gamss.android.domain.analytics.AnalyticsProperty
import com.gamss.android.domain.analytics.AnalyticsTracker
import com.gamss.android.domain.card.DeleteCardUseCase
import com.gamss.android.domain.card.DeleteCardsByEmotionUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class CardDeleteViewModel @Inject constructor(
    private val deleteCardsByEmotion: DeleteCardsByEmotionUseCase,
    private val deleteCard: DeleteCardUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel(), ContainerHost<CardDeleteState, CardDeleteSideEffect> {

    override val container = container<CardDeleteState, CardDeleteSideEffect>(CardDeleteState())

    /**
     * 누를 때마다 종이가 한 단계씩 내려간다. 끝까지 내려가면 그 시점에 실제 삭제를 요청한다.
     *
     * @param cardId 파쇄할 카드. null 이면 [emotion] 칸의 카드를 모두 삭제한다.
     */
    fun onShredTap(emotion: EmotionCharacter, cardId: Long?) = intent {
        if (state.isDeleting || state.shredTapCount >= SHRED_TOTAL_TAPS) return@intent

        val nextTapCount = state.shredTapCount + 1
        reduce { state.copy(shredTapCount = nextTapCount) }
        val properties = mapOf(
            AnalyticsProperty.EMOTION to emotion.name.lowercase(),
            AnalyticsProperty.SCOPE to if (cardId == null) SCOPE_EMOTION_ALL else SCOPE_SINGLE,
        )
        analyticsTracker.track(
            AnalyticsEvent.CARD_SHRED_TAP,
            properties + (AnalyticsProperty.TAP_COUNT to nextTapCount),
        )
        if (nextTapCount < SHRED_TOTAL_TAPS) return@intent

        reduce { state.copy(isDeleting = true) }
        val result = coroutineScope {
            val minimumHold = launch { delay(SHRED_COMPLETE_HOLD_MS) }
            val outcome = if (cardId == null) deleteCardsByEmotion(emotion) else deleteCard(cardId)
            minimumHold.join()
            outcome
        }
        when (result) {
            is AppResult.Success -> {
                reduce { state.copy(isDeleting = false, isCompleted = true) }
                analyticsTracker.track(AnalyticsEvent.CARD_SHRED_SUCCESS, properties)
                postSideEffect(CardDeleteSideEffect.ShredSuccess)
            }

            is AppResult.Failure -> {
                reduce { state.copy(isDeleting = false, shredTapCount = 0) }
                analyticsTracker.track(AnalyticsEvent.CARD_SHRED_FAIL, properties)
                postSideEffect(CardDeleteSideEffect.ShredFailure)
            }
        }
    }

    private companion object {
        const val SCOPE_SINGLE = "single"
        const val SCOPE_EMOTION_ALL = "emotion_all"
    }
}
