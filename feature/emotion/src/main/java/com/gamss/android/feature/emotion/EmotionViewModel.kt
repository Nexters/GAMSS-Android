package com.gamss.android.feature.emotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import com.gamss.android.domain.emotion.EmotionClassifier
import com.gamss.android.domain.safety.DetectRiskInTextUseCase
import com.gamss.android.domain.safety.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class EmotionViewModel @Inject constructor(
    private val classifyUserEmotion: ClassifyUserEmotionUseCase,
    private val detectRiskInText: DetectRiskInTextUseCase,
    private val classifier: EmotionClassifier,
) : ViewModel(),
    ContainerHost<EmotionState, Nothing> {

    override val container = container<EmotionState, Nothing>(EmotionState())

    init {
        // 결과를 기다리지 않는다 — 화면 진입 시점부터 모델 다운로드를 미리 걸어둬 실제 분석 요청
        // 시점엔 이미 받아져 있을 확률을 높이는 순수 최적화용 호출이다.
        viewModelScope.launch { classifier.prefetch() }
    }

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
