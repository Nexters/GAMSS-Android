package com.gamss.android.data.safety.model

import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.RiskLexicon
import com.gamss.android.domain.safety.RiskTerm
import com.gamss.android.domain.safety.SupportAgency
import kotlinx.serialization.Serializable

@Serializable
internal data class RiskLexiconDto(
    val version: Int = 0,
    val terms: List<RiskTermDto> = emptyList(),
    val safePhrases: List<String> = emptyList(),
    val agencies: List<SupportAgencyDto> = emptyList(),
)

@Serializable
internal data class RiskTermDto(
    val term: String = "",
    val level: String = "",
)

@Serializable
internal data class SupportAgencyDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val phoneNumber: String? = null,
    val url: String? = null,
    val priority: Int = DEFAULT_PRIORITY,
)

internal fun RiskLexiconDto.toDomain(): RiskLexicon = RiskLexicon(
    version = version,
    terms = terms.filter { it.term.isNotBlank() }.map { it.toDomain() },
    safePhrases = safePhrases.filter { it.isNotBlank() },
    agencies = agencies.filter { it.name.isNotBlank() }.map { it.toDomain() },
)

private fun RiskTermDto.toDomain(): RiskTerm = RiskTerm(
    term = term,
    level = when (level.uppercase()) {
        RiskLevel.CRITICAL.name -> RiskLevel.CRITICAL
        RiskLevel.NONE.name -> RiskLevel.NONE
        else -> RiskLevel.WARNING
    },
)

private fun SupportAgencyDto.toDomain(): SupportAgency = SupportAgency(
    id = id,
    name = name,
    description = description,
    phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
    url = url?.takeIf { it.isNotBlank() },
    priority = priority,
)

private const val DEFAULT_PRIORITY = Int.MAX_VALUE
