package com.gamss.android.data.emotion

/**
 * WordPiece 인코더 분류기 하나를 정의하는 설정.
 * 새 분류 모델(감정/위험 등) 추가는 이 spec 한 건을 늘리는 것으로 끝난다 — 엔진 코드는 재사용된다.
 *
 * @param packName 모델/토크나이저를 담은 Play Asset Delivery on-demand 애셋팩 이름.
 * @param labels 인덱스가 곧 모델 출력 인덱스(id2label). 순서가 계약이다.
 */
internal data class ClassifierSpec(
    val packName: String,
    val modelAsset: String,
    val tokenizerAsset: String,
    val labels: List<String>,
    val seqLen: Int = 128,
)
