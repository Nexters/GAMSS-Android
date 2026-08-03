package com.gamss.android.domain.safety

/**
 * matchedTerms 는 등급 판정과 테스트에만 쓴다. 원문과 매칭된 단어는 로그·분석 이벤트에 남기지 않는다.
 */
data class RiskDetection(
    val level: RiskLevel,
    val matchedTerms: List<String>,
    val agencies: List<SupportAgency>,
) {
    val shouldBlock: Boolean get() = level == RiskLevel.CRITICAL

    companion object {
        val None = RiskDetection(
            level = RiskLevel.NONE,
            matchedTerms = emptyList(),
            agencies = emptyList(),
        )
    }
}
