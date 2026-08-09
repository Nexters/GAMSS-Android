package com.gamss.android.domain.safety

data class RiskLexicon(
    val version: Int,
    val terms: List<RiskTerm>,
    val safePhrases: List<String>,
    val agencies: List<SupportAgency>,
)
