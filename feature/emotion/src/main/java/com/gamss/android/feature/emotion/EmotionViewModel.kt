package com.gamss.android.feature.emotion

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import com.gamss.android.domain.safety.DetectRiskInTextUseCase
import com.gamss.android.domain.safety.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class EmotionViewModel @Inject constructor(
    private val classifyUserEmotion: ClassifyUserEmotionUseCase,
    private val detectRiskInText: DetectRiskInTextUseCase,
) : ViewModel(),
    ContainerHost<EmotionState, Nothing> {

    override val container = container<EmotionState, Nothing>(EmotionState())

    fun onInputChange(text: String) = intent {
        reduce { state.copy(input = text) }
    }

    fun onAnalyze() = intent {
        if (state.isRunning) return@intent
        reduce { state.copy(isRunning = true, result = null, notRecognized = false, error = null) }

        val detection = detectRiskInText(state.input)
        if (detection.level != RiskLevel.NONE) {
            reduce { state.copy(riskDetection = detection) }
            if (detection.shouldBlock) {
                reduce { state.copy(isRunning = false) }
                return@intent
            }
        }

        val utterances = listOf(state.input.trim())
        when (val result = classifyUserEmotion(utterances)) {
            is AppResult.Success -> reduce {
                state.copy(isRunning = false, result = result.data, notRecognized = result.data == null)
            }
            // 원문(사용자 입력)을 노출/기록하지 않고 일반 메시지만 노출한다.
            is AppResult.Failure -> reduce { state.copy(isRunning = false, error = "감정 분석에 실패했어요") }
        }
    }

    fun onRiskGuidanceDismiss() = intent {
        reduce { state.copy(riskDetection = null) }
    }
}
