package com.gamss.android.feature.emotion

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class EmotionViewModel @Inject constructor(
    private val classifyUserEmotion: ClassifyUserEmotionUseCase,
) : ViewModel(),
    ContainerHost<EmotionState, EmotionSideEffect> {

    override val container = container<EmotionState, EmotionSideEffect>(EmotionState())

    fun onInputChange(text: String) = intent {
        reduce { state.copy(input = text) }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun onAnalyze() = intent {
        if (state.isRunning) return@intent
        val utterances = listOf(state.input.trim())
        reduce { state.copy(isRunning = true, result = null, notRecognized = false, error = null) }
        try {
            val result = classifyUserEmotion(utterances)
            reduce { state.copy(isRunning = false, result = result, notRecognized = result == null) }
        } catch (t: Throwable) {
            // 원문(사용자 입력)을 노출/기록하지 않고 일반 메시지만 노출한다.
            reduce { state.copy(isRunning = false, error = "감정 분석에 실패했어요") }
        }
    }
}
