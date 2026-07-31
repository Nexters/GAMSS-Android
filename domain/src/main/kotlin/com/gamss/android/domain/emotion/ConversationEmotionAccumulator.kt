package com.gamss.android.domain.emotion

import javax.inject.Inject

/**
 * 발화가 생길 때마다 분류해 점수를 누적한다. 종료 시점에 몰아서 분류하지 않으려는 용도이고,
 * 결과는 [ClassifyUserEmotionUseCase] 로 한꺼번에 계산한 것과 동일하다.
 *
 * 가변 상태를 들고 있어 대화 하나에 인스턴스 하나여야 한다. 스코프 애노테이션을 붙이면
 * (예: `@Singleton`) 대화 간에 점수가 섞이므로 무스코프로 두어야 한다. 스레드 안전하지 않다.
 */
class ConversationEmotionAccumulator @Inject constructor(
    private val classifier: EmotionClassifier,
) {
    private val summed = LinkedHashMap<String, Float>()
    private var utteranceCount = 0

    /** 공백 발화는 분류 대상이 아니라 건너뛴다. */
    suspend fun add(utterance: String) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return
        classifier.classify(trimmed).scores.forEach { (label, score) ->
            summed[label] = (summed[label] ?: 0f) + score
        }
        utteranceCount++
    }

    suspend fun addAll(utterances: List<String>) {
        utterances.forEach { add(it) }
    }

    fun result(): EmotionResult? = aggregateEmotion(summed, utteranceCount)

    fun reset() {
        summed.clear()
        utteranceCount = 0
    }
}
