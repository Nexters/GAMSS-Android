package com.gamss.android.data.tokenizer

/**
 * tokenizer.json 의 added_tokens 를 정규화 이전에 원문에서 떼어낸다. kobart 는 이모티콘·이모지가
 * 122개 들어 있어 이 단계를 빼면 일기 본문이 통째로 다르게 토큰화된다.
 *
 * 두 tokenizer.json 모두 추가 토큰이 single_word·lstrip·rstrip·normalized 전부 false 라
 * 원문을 leftmost-longest 로 훑는 것으로 충분하다.
 */
internal class AddedVocabulary private constructor(
    private val candidatesByFirstChar: Map<Char, List<Candidate>>,
) {

    private class Candidate(val content: String, val id: Int)

    sealed interface Segment {
        /** 추가 토큰 사이에 남은 구간. 일반 파이프라인을 거친다. */
        class Plain(val text: String) : Segment

        /** 추가 토큰. 더 쪼개지 않는다. */
        class Added(val id: Int) : Segment
    }

    /** leftmost-longest. */
    fun split(text: String): List<Segment> {
        if (text.isEmpty()) return emptyList()

        val segments = mutableListOf<Segment>()
        val pending = StringBuilder()
        fun flushPending() {
            if (pending.isNotEmpty()) {
                segments.add(Segment.Plain(pending.toString()))
                pending.setLength(0)
            }
        }

        var index = 0
        while (index < text.length) {
            val matched = longestMatchAt(text, index)
            if (matched == null) {
                val codePoint = text.codePointAt(index)
                val width = Character.charCount(codePoint)
                pending.append(text, index, index + width)
                index += width
            } else {
                flushPending()
                segments.add(Segment.Added(matched.id))
                index += matched.content.length
            }
        }
        flushPending()
        return segments
    }

    private fun longestMatchAt(text: String, index: Int): Candidate? =
        // 후보가 길이 내림차순이라 처음 걸리는 것이 가장 길다.
        candidatesByFirstChar[text[index]]?.firstOrNull { text.startsWith(it.content, index) }

    companion object {
        fun of(tokens: Map<String, Int>): AddedVocabulary = AddedVocabulary(
            tokens.asSequence()
                .filter { it.key.isNotEmpty() }
                .map { Candidate(it.key, it.value) }
                .groupBy { it.content.first() }
                .mapValues { (_, candidates) -> candidates.sortedByDescending { it.content.length } },
        )
    }
}
