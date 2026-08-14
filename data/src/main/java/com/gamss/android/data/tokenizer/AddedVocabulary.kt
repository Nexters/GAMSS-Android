package com.gamss.android.data.tokenizer

/**
 * tokenizer.json 의 added_tokens 를 원문에서 먼저 떼어낸다.
 *
 * HuggingFace 는 정규화·사전분할 이전에 추가 토큰부터 추출하고, 잘라낸 조각만 일반 파이프라인에 태운다.
 * kobart 쪽은 `:-)` 같은 이모티콘과 이모지가 여기에 122개 들어 있어서, 이 단계를 빼면 일기 본문의
 * 이모티콘이 통째로 다르게 토큰화된다.
 *
 * 지금 쓰는 두 tokenizer.json 은 추가 토큰이 전부 single_word·lstrip·rstrip 이 꺼져 있고
 * normalized 도 false 라, 원문을 그대로 leftmost-longest 로 훑는 것으로 충분하다.
 */
internal class AddedVocabulary private constructor(
    private val candidatesByFirstChar: Map<Char, List<Candidate>>,
) {

    private class Candidate(val content: String, val id: Int)

    sealed interface Segment {
        /** 추가 토큰 사이에 남은 구간. 정규화·사전분할·BPE 를 거친다. */
        class Plain(val text: String) : Segment

        /** 추가 토큰. 더 쪼개지 않고 id 를 그대로 쓴다. */
        class Added(val id: Int) : Segment
    }

    /** 겹치지 않게 왼쪽부터 훑되, 같은 자리에서는 가장 긴 토큰을 고른다. */
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
        // 후보는 길이 내림차순이라 처음 걸리는 것이 가장 긴 것이다.
        candidatesByFirstChar[text[index]]?.firstOrNull { text.startsWith(it.content, index) }

    companion object {
        /** 추가 토큰이 하나도 없으면 원문을 그대로 통과시킨다. */
        fun of(tokens: Map<String, Int>): AddedVocabulary = AddedVocabulary(
            tokens.asSequence()
                .filter { it.key.isNotEmpty() }
                .map { Candidate(it.key, it.value) }
                .groupBy { it.content.first() }
                .mapValues { (_, candidates) -> candidates.sortedByDescending { it.content.length } },
        )
    }
}
