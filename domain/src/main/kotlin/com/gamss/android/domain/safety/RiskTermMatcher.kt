package com.gamss.android.domain.safety

import java.text.Normalizer
import javax.inject.Inject

/**
 * 공백과 구두점을 지운 뒤 부분 문자열로 찾으므로 "죽고 싶다", "죽.고.싶.다" 를 한 표현으로 본다.
 *
 * 두 글자 이하 표현은 어절 경계에서 시작할 때만 인정한다. 공백을 지우면 "과자 살까" 안에
 * "자살" 이, "혼자 살아보니" 안에도 "자살" 이 생겨 평범한 일기가 차단되기 때문이다.
 * safePhrases 구간은 먼저 마스킹해 "배고파 죽겠다" 류의 관용 표현을 걸러낸다.
 */
class RiskTermMatcher @Inject constructor() {

    fun match(text: String, lexicon: RiskLexicon): RiskDetection {
        val matched = findMatchedTerms(text, lexicon)
        return if (matched.isEmpty()) {
            RiskDetection.None
        } else {
            RiskDetection(
                level = if (matched.any { it.level == RiskLevel.CRITICAL }) {
                    RiskLevel.CRITICAL
                } else {
                    RiskLevel.WARNING
                },
                matchedTerms = matched.map { it.term },
                agencies = lexicon.agencies.sortedBy { it.priority },
            )
        }
    }

    private fun findMatchedTerms(text: String, lexicon: RiskLexicon): List<RiskTerm> {
        val normalized = normalize(text)
        if (normalized.value.isEmpty()) return emptyList()

        val masked = maskSafePhrases(normalized.value, lexicon.safePhrases)
        return lexicon.terms.filter { it.matches(masked, normalized.wordStarts) }
    }

    private fun RiskTerm.matches(maskedText: String, wordStarts: Set<Int>): Boolean {
        val normalizedTerm = normalize(term).value
        return when {
            level == RiskLevel.NONE -> false
            normalizedTerm.isEmpty() -> false
            normalizedTerm.length > SHORT_TERM_LENGTH -> maskedText.contains(normalizedTerm)
            else -> maskedText.occurrencesOf(normalizedTerm).any { it in wordStarts }
        }
    }

    /**
     * 알파벳과 숫자만 남긴 문자열, 그리고 각 어절의 첫 글자가 그 문자열에서 갖는 위치.
     */
    private class NormalizedText(val value: String, val wordStarts: Set<Int>)

    private fun normalize(text: String): NormalizedText {
        val builder = StringBuilder()
        val wordStarts = mutableSetOf<Int>()
        var atWordStart = true
        for (character in Normalizer.normalize(text, Normalizer.Form.NFKC)) {
            if (character.isLetterOrDigit()) {
                if (atWordStart) wordStarts.add(builder.length)
                builder.append(character.lowercaseChar())
                atWordStart = false
            } else {
                atWordStart = true
            }
        }
        return NormalizedText(builder.toString(), wordStarts)
    }

    private fun maskSafePhrases(normalized: String, safePhrases: List<String>): String =
        safePhrases.fold(normalized) { text, phrase ->
            val normalizedPhrase = normalize(phrase).value
            if (normalizedPhrase.isEmpty()) {
                text
            } else {
                text.replace(normalizedPhrase, MASK.toString().repeat(normalizedPhrase.length))
            }
        }

    private fun String.occurrencesOf(value: String): List<Int> {
        val positions = mutableListOf<Int>()
        var index = indexOf(value)
        while (index >= 0) {
            positions += index
            index = indexOf(value, index + 1)
        }
        return positions
    }

    private companion object {
        /** 어떤 위험 표현과도 겹치지 않는 대체 문자. 마스킹 후에도 위치가 밀리지 않게 길이를 보존한다. */
        const val MASK = ' '

        /** 이 길이 이하의 표현은 어절 경계에서 시작할 때만 인정한다. */
        const val SHORT_TERM_LENGTH = 2
    }
}
