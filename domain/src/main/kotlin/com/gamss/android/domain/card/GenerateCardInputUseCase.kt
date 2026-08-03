package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.map
import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import com.gamss.android.domain.emotion.EmotionResult
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/**
 * 대화의 USER 발화 목록 → 카드 생성 입력. 발화 전체를 한꺼번에 분류하는 배치 경로이고,
 * 지금은 파이프라인 검증용(CardDebugActivity)에서만 쓴다. 실제 카드 생성은 대화 중에
 * [com.gamss.android.domain.emotion.ConversationEmotionAccumulator] 로 감정을 누적한 뒤
 * [CreateCardUseCase] 로 보낸다.
 *
 * 감정 분류와 요약을 원문 USER 발화에서 각각 독립적으로 생성한다(요약을 감정 입력으로 쓰는
 * 체이닝 없음. 요약은 감정 신호를 침식하므로).
 */
class GenerateCardInputUseCase @Inject constructor(
    private val classifyUserEmotion: ClassifyUserEmotionUseCase,
    private val summarizeDiary: SummarizeDiaryUseCase,
) : UseCase<List<String>, AppResult<CardInput?>> {

    override suspend fun invoke(params: List<String>): AppResult<CardInput?> =
        when (val emotion = classifyUserEmotion(params)) {
            is AppResult.Failure -> emotion
            is AppResult.Success -> emotion.data?.let { withSummary(it, params) } ?: AppResult.Success(null)
        }

    private suspend fun withSummary(emotion: EmotionResult, userUtterances: List<String>): AppResult<CardInput?> =
        summarizeDiary(userUtterances).map { summary ->
            CardInput(
                emotion = emotion.label,
                character = emotion.character,
                summary = summary,
            )
        }
}
