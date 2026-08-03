package com.gamss.android.domain.safety

/**
 * version 은 앱 내장 사전과 원격 사전의 우열을 가리는 기준이다. 원격이 더 클 때만 갱신한다.
 */
data class RiskLexicon(
    val version: Int,
    val terms: List<RiskTerm>,
    val safePhrases: List<String>,
    val agencies: List<SupportAgency>,
)
