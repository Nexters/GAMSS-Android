package com.gamss.android.domain.card

import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import javax.inject.Inject

/**
 * 대화의 USER 발화 목록 → 카드 생성 입력. 발화 전체를 한꺼번에 분류하는 배치 경로이고,
 * 지금은 파이프라인 검증용(CardDebugActivity)에서만 쓴다. 실제 카드 생성은 대화 중에
 * [com.gamss.android.domain.emotion.ConversationEmotionAccumulator] 로 감정을 누적한 뒤
 * [CreateCardUseCase] 로 보낸다.
 *
 * 감정 분류와 요약을 원문 USER 발화에서 각각 독립적으로 생성한다(요약을 감정 입력으로 쓰는
 * 체이닝 없음. 요약은 감정 신호를 침식하므로). USER 발화가 없어 감정을 낼 수 없으면 null.
 */
class GenerateCardInputUseCase @Inject constructor(
    private val classifyUserEmotion: ClassifyUserEmotionUseCase,
    private val summarizeDiary: SummarizeDiaryUseCase,
) {
    suspend operator fun invoke(userUtterances: List<String>): CardInput? {
        val emotionResult = classifyUserEmotion(userUtterances) ?: return null
        val summary = summarizeDiary(userUtterances)
        return CardInput(
            emotion = emotionResult.label,
            character = emotionResult.character,
            summary = summary,
        )
    }
}
