package com.gamss.android.domain.emotion

/**
 * 발화별 점수 합 → 대표 감정. 배치 분류와 증분 누적이 같은 결과를 내도록 집계는 여기 한 곳에만 둔다.
 * 발화 수로 나누는 건 표시용 확신도라 argmax 는 바뀌지 않는다.
 */
internal fun aggregateEmotion(summedScores: Map<String, Float>, utteranceCount: Int): EmotionResult? {
    if (utteranceCount <= 0 || summedScores.isEmpty()) return null

    val normalized = summedScores.mapValues { it.value / utteranceCount }
    return normalized.maxByOrNull { it.value }?.let { top ->
        val label = EmotionLabel.fromKoLabel(top.key)
        EmotionResult(
            label = label,
            character = EmotionCharacter.fromEmotionLabel(label),
            distribution = ClassificationResult(top.key, top.value, normalized),
        )
    }
}
