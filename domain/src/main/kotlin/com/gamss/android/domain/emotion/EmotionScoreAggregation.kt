package com.gamss.android.domain.emotion

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
