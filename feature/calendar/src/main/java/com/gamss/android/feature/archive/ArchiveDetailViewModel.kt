package com.gamss.android.feature.archive

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.domain.card.GetCardsByDateUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ArchiveDetailViewModel @Inject constructor(
    private val getCardsByDate: GetCardsByDateUseCase,
) : ViewModel(), ContainerHost<ArchiveDetailState, Nothing> {

    override val container = container<ArchiveDetailState, Nothing>(ArchiveDetailState())

    fun load(emotion: EmotionCharacter) = intent {
        if (state.emotion == emotion && !state.isLoading) return@intent

        reduce { ArchiveDetailState(emotion = emotion) }
        when (val result = getCardsByDate(LocalDate.now(KoreanTimeZone))) {
            is AppResult.Success -> reduce {
                state.copy(
                    isLoading = false,
                    cards = result.data.filter { it.character == emotion },
                )
            }

            is AppResult.Failure -> reduce { state.copy(isLoading = false, loadFailed = true) }
        }
    }
}
