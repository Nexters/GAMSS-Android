package com.gamss.android.domain.emotion

/** 텍스트 → 감정 분류 결과. 온디바이스 구현이 이 포트를 만족한다. */
interface EmotionClassifier {
    suspend fun classify(text: String): ClassificationResult
}
