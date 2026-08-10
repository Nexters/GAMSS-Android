package com.gamss.android.domain.emotion

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/**
 * USER 발화 목록 → 카드에 표시할 대표 감정 하나와 그 감정을 대표하는 캐릭터.
 *
 * 각 발화를 요약 없이 그대로 분류하고 softmax 점수 분포를 발화 수만큼 합산해 argmax 를 대표로 삼는다.
 * - 한 발화가 튀어도 대화 전체의 지배적 감정이 뽑힌다(다수결·최고신뢰보다 안정적).
 * - 요약을 거치지 않아 감정 어휘 신호가 보존된다(요약→감정 체이닝의 신호 손실 회피).
 * 대표 감정(6종)은 [EmotionCharacter.fromEmotionLabel] 로 캐릭터에 1:1 매핑한다.
 */
class ClassifyUserEmotionUseCase @Inject constructor(
    private val classifier: EmotionClassifier,
) : UseCase<List<String>, AppResult<EmotionResult?>> {

    override suspend fun invoke(params: List<String>): AppResult<EmotionResult?> = AppResult.of {
        val utterances = params.map { it.trim() }.filter { it.isNotEmpty() }
        if (utterances.isEmpty()) return@of null

        val summed = LinkedHashMap<String, Float>()
        for (utterance in utterances) {
            classifier.classify(utterance).scores.forEach { (label, score) ->
                summed[label] = (summed[label] ?: 0f) + score
            }
        }
        aggregateEmotion(summed, utterances.size)
    }
}
