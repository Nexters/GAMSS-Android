package com.gamss.android.domain.conversation

const val MAX_CONVERSATION_TITLE_LENGTH = 20

private const val MIN_TITLE_LENGTH = 6

private const val MIN_MEANINGFUL_LENGTH = 4

private const val ELLIPSIS = "…"

private val SENTENCE_DELIMITER = Regex("""(?:\.(?!\d)|[!?…。！？\n\r])+""")

private val WHITESPACE = Regex("""[\s 　]+""")

private val TRAILING_MARKS = Regex("""[,~\-ㅋㅎㅠㅜ.]+$""")

private val INTERJECTIONS = setOf(
    "아", "아아", "어", "음", "하", "하아", "허", "헐", "와", "우와", "에휴", "아오", "아우", "아이고", "휴", "참",
    "진짜", "정말", "너무", "완전", "존나", "매우", "몹시", "엄청", "되게", "그냥", "좀", "막", "약간", "많이",
)

private val FILLER_PREFIXES = listOf(
    "이", "그", "저", "아", "어", "하", "진짜", "정말", "너무", "완전", "개", "존나", "핵", "겁나", "졸라", "왕",
)

private val WEAK_WORDS = setOf("오늘", "지금", "방금", "아까", "이제")

private val EMOTION_STEMS = listOf(
    "짜증", "화나", "화났", "빡쳐", "빡치", "열받", "힘들", "힘드", "우울", "슬프", "슬퍼", "슬펐", "속상",
    "억울", "답답", "지치", "지쳐", "지친", "서럽", "서운", "불안", "무섭", "두렵", "외롭", "허무", "허탈",
    "괴롭", "괴로", "미치", "미쳐", "죽겠", "싫", "귀찮", "울었", "행복", "기쁘", "기뻐", "기뻤", "좋았",
    "신난", "설레", "뿌듯", "고민",
)

private val EMOTION_ENDINGS = listOf(
    "다", "다고", "더라", "네", "어", "아", "워", "여", "지", "고", "군", "구나", "은데", "는데",
    "나", "나네", "난다", "났어", "나서", "어서", "아서", "워서", "었어", "았어", "웠어",
    "해", "해서", "했어", "한다", "하다", "겠어", "겠다", "된다", "돼", "돼서", "됐어",
)

private val BARE_EMOTIONS = setOf(
    "짜증", "짜증나", "우울", "화나", "화남", "슬퍼", "기뻐", "빡침", "현타", "멘붕", "귀찮", "싫",
)

private val EMOTION_WORDS: Set<String> = EMOTION_STEMS
    .flatMapTo(BARE_EMOTIONS.toMutableSet()) { stem -> EMOTION_ENDINGS.map { stem + it } }

fun conversationTitleFrom(seed: String): String? {
    val sentences = seed.splitSentences()
    if (sentences.isEmpty()) return null

    val candidates = sentences
        .mapNotNull { sentence -> titleCandidate(sentence.text)?.let { sentence to it } }
        .sortedBy { (sentence) -> sentence.isQuestion }
        .map { (_, title) -> title }

    val title = candidates.firstOrNull(String::isMeaningfulTitle)
        ?: candidates.firstOrNull()
        ?: sentences.map(Sentence::text).headline()
    return title.ellipsize()
}

private class Sentence(val text: String, val isQuestion: Boolean)

private fun String.splitSentences(): List<Sentence> {
    var start = 0
    return buildList {
        for (boundary in SENTENCE_DELIMITER.findAll(this@splitSentences)) {
            add(Sentence(substring(start, boundary.range.first), boundary.value.any(Char::isQuestionMark)))
            start = boundary.range.last + 1
        }
        add(Sentence(substring(start), false))
    }
        .mapNotNull { sentence ->
            sentence.text.replace(WHITESPACE, " ").trim()
                .takeIf(String::isNotEmpty)
                ?.let { Sentence(it, sentence.isQuestion) }
        }
}

private fun Char.isQuestionMark(): Boolean = this == '?' || this == '？'

private fun titleCandidate(sentence: String): String? {
    val words = sentence.split(' ')
    val startIndex = words.indexOfFirst(::isFactual)
    return startIndex.takeIf { it >= 0 }
        ?.let(words::drop)
        ?.joinToString(" ")
}

private fun isFactual(word: String): Boolean {
    val bare = word.replace(TRAILING_MARKS, "")
    return bare.isNotEmpty() &&
        bare !in WEAK_WORDS &&
        !bare.isFiller() &&
        !bare.stripFillerPrefixes().isFiller()
}

private fun String.isFiller(): Boolean = this in INTERJECTIONS || this in EMOTION_WORDS

private fun String.stripFillerPrefixes(): String {
    var rest = this
    while (true) {
        val prefix = FILLER_PREFIXES.firstOrNull { rest.length > it.length && rest.startsWith(it) } ?: return rest
        rest = rest.removePrefix(prefix)
    }
}

private fun List<String>.headline(): String {
    return drop(1).fold(first()) { headline, sentence ->
        headline.takeIf { it.length >= MIN_TITLE_LENGTH } ?: "$headline $sentence"
    }
}

private fun String.ellipsize(): String {
    if (length <= MAX_CONVERSATION_TITLE_LENGTH) return this
    val limit = MAX_CONVERSATION_TITLE_LENGTH - ELLIPSIS.length
    val end = if (this[limit - 1].isHighSurrogate()) limit - 1 else limit
    return take(end).trimEnd() + ELLIPSIS
}

private fun String.isMeaningfulTitle(): Boolean = length >= MIN_MEANINGFUL_LENGTH
