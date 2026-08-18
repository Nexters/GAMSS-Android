package com.gamss.android.feature.carddelete

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.DeleteAllCardsUseCase
import com.gamss.android.domain.card.DeleteCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class CardDeleteViewModel @Inject constructor(
    private val deleteCard: DeleteCardUseCase,
    private val deleteAllCards: DeleteAllCardsUseCase,
) : ViewModel(), ContainerHost<CardDeleteState, CardDeleteSideEffect> {

    override val container = container<CardDeleteState, CardDeleteSideEffect>(CardDeleteState())

    /** 누를 때마다 종이가 한 단계씩 내려간다. 끝까지 내려가면 [cardId] 한 장을, null 이면 전부 삭제한다. */
    fun onShredTap(cardId: Long?) = intent {
        if (state.isDeleting || state.shredTapCount >= SHRED_TOTAL_TAPS) return@intent

        val nextTapCount = state.shredTapCount + 1
        reduce { state.copy(shredTapCount = nextTapCount) }
        if (nextTapCount < SHRED_TOTAL_TAPS) return@intent

        reduce { state.copy(isDeleting = true) }
        val result = coroutineScope {
            val minimumHold = launch { delay(SHRED_COMPLETE_HOLD_MS) }
            val outcome = if (cardId == null) deleteAllCards() else deleteCard(cardId)
            minimumHold.join()
            outcome
        }
        when (result) {
            is AppResult.Success -> {
                reduce { state.copy(isDeleting = false, isCompleted = true) }
                postSideEffect(CardDeleteSideEffect.ShredSuccess)
            }

            is AppResult.Failure -> {
                reduce { state.copy(isDeleting = false, shredTapCount = 0) }
                postSideEffect(CardDeleteSideEffect.ShredFailure)
            }
        }
    }
}
