package com.gamss.android.feature.emotion

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.emotion.AnalyzeConversationEmotionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class EmotionViewModel @Inject constructor(
    private val analyzeConversationEmotion: AnalyzeConversationEmotionUseCase,
) : ViewModel(),
    ContainerHost<EmotionState, EmotionSideEffect> {

    override val container = container<EmotionState, EmotionSideEffect>(EmotionState())

    fun onInputChange(text: String) = intent {
        reduce { state.copy(input = text) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onAnalyze() = intent {
        if (state.isRunning) return@intent
        val input = state.input
        reduce { state.copy(isRunning = true, result = null, notRecognized = false, error = null) }
        try {
            val result = analyzeConversationEmotion(input)
            reduce { state.copy(isRunning = false, result = result, notRecognized = result == null) }
        } catch (t: Throwable) {
            reduce { state.copy(isRunning = false, error = t.message ?: "알 수 없는 오류") }
        }
    }
}
