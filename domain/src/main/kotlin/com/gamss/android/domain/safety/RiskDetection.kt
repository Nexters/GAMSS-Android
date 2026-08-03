package com.gamss.android.domain.safety

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
