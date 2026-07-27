package com.gamss.android.domain.emotion

/**
 * 대표 감정 결과.
 *
 * @property label 대화 전체의 대표 감정
 * @property character 대표 감정을 대표하는 캐릭터(1:1)
 * @property distribution 발화 집계 점수 분포. 감정 화면의 분포 표시용 원자료이며 상위(카드)로는 넘기지 않는다.
 */
data class EmotionResult(
    val label: EmotionLabel,
    val character: EmotionCharacter,
    val distribution: ClassificationResult,
)
