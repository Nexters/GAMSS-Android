package com.gamss.android.feature.calendar

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.DeleteCardUseCase
import com.gamss.android.domain.card.GetCardsByDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCardsByDate: GetCardsByDateUseCase,
    private val deleteCard: DeleteCardUseCase,
) :
    ViewModel(),
    ContainerHost<CalendarState, CalendarSideEffect> {

    override val container = container<CalendarState, CalendarSideEffect>(
        CalendarState(today = LocalDate.now()),
    )

    private var cardLoadJob: Job? = null

    fun selectDate(date: LocalDate) = intent {
        if (date == state.selectedDate) {
            cardLoadJob?.cancel()
            reduce { state.copy(selectedDate = null, cardLoadState = CalendarCardLoadState.Idle) }
            return@intent
        }

        startCardLoad(date)
    }

    fun retrySelectedDate() = intent {
        state.selectedDate?.let { startCardLoad(it) }
    }

    fun selectCard(card: Card) = intent {
        reduce { state.copy(selectedCard = card) }
    }

    fun dismissCardDetail() = intent {
        reduce { state.copy(selectedCard = null) }
    }

    fun discardSelectedCard() = intent {
        val card = state.selectedCard ?: return@intent
        reduce { state.copy(selectedCard = null) }

        when (deleteCard(card.id)) {
            is AppResult.Success -> reduce { state.copy(cardLoadState = state.cardLoadState.withoutCard(card.id)) }
            is AppResult.Failure -> postSideEffect(CalendarSideEffect.CardDiscardFailed)
        }
    }

    fun viewSelectedConversation() = intent {
        val conversationId = state.selectedCard?.conversationId ?: return@intent
        reduce { state.copy(selectedCard = null) }
        postSideEffect(CalendarSideEffect.OpenChatRoom(conversationId))
    }

    private suspend fun Syntax<CalendarState, CalendarSideEffect>.startCardLoad(date: LocalDate) {
        cardLoadJob?.cancelAndJoin()
        reduce { state.copy(selectedDate = date, cardLoadState = CalendarCardLoadState.Loading) }

        cardLoadJob = container.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val loadState = when (val result = getCardsByDate(date)) {
                is AppResult.Success ->
                    result.data
                        .takeIf { it.isNotEmpty() }
                        ?.let(CalendarCardLoadState::Content)
                        ?: CalendarCardLoadState.Empty
                is AppResult.Failure ->
                    CalendarCardLoadState.Error
            }

            intent {
                if (state.selectedDate == date) {
                    reduce { state.copy(cardLoadState = loadState) }
                }
            }
        }
    }
}
