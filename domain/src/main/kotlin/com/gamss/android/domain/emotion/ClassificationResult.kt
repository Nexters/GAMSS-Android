package com.gamss.android.domain.emotion

/**
 * 분류 결과. 최상위 라벨/확신도뿐 아니라 전체 클래스 점수를 보존해
 * 확신도 임계값·대화 전체 집계 같은 후처리를 상위 계층에서 할 수 있게 한다.
 */
data class ClassificationResult(
    val topLabel: String,
    val confidence: Float,
    val scores: Map<String, Float>,
)
