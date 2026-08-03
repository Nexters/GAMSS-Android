package com.gamss.android.domain.safety

interface RiskLexiconRepository {

    suspend fun getLexicon(): RiskLexicon

    suspend fun refresh()
}
