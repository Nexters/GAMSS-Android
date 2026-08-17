package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.GetCardsByMonthUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ArchiveDetailViewModel @Inject constructor(
    private val getCardsByMonth: GetCardsByMonthUseCase,
) : ViewModel(), ContainerHost<ArchiveDetailState, Nothing> {

    override val container = container<ArchiveDetailState, Nothing>(
        ArchiveDetailState(yearMonth = YearMonth.now(KoreanTimeZone)),
    )

    fun load(emotion: EmotionCharacter) = intent {
        if (state.emotion == emotion) return@intent

        reduce { state.copy(emotion = emotion) }
        val result = getCardsByMonth(state.yearMonth)
        reduce { state.withCards(result, emotion) }
    }

    fun showMonthPicker() = intent {
        reduce { state.copy(isMonthPickerVisible = true) }
    }

    fun dismissMonthPicker() = intent {
        reduce { state.copy(isMonthPickerVisible = false) }
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
                isLoading = true,
                cards = emptyList(),
                loadFailed = false,
            )
        }
        val result = getCardsByMonth(yearMonth)
        reduce { state.withCards(result, emotion) }
    }
}

private fun ArchiveDetailState.withCards(
    result: AppResult<List<CardEntry>>,
    emotion: EmotionCharacter,
): ArchiveDetailState = when (result) {
    is AppResult.Success -> copy(isLoading = false, cards = result.data.filter { it.character == emotion })
    is AppResult.Failure -> copy(isLoading = false, loadFailed = true)
}
