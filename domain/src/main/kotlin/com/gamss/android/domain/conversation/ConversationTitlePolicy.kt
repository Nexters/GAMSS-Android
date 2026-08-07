package com.gamss.android.domain.conversation

/** 목록에서 방을 한눈에 구분할 길이. 서버 상한(100자)보다 훨씬 짧게 잡는다. */
const val MAX_CONVERSATION_TITLE_LENGTH = 20

/** 이어 붙일지 판단하는 기준. 이보다 짧은 문장은 한 마디라 제목 구실을 못 한다. */
private const val MIN_TITLE_LENGTH = 6

/** 제목으로 쓸 만한 최소 길이. "팀장 탓"처럼 짧고 정확한 사실을 뒤 문장에 밀리게 하지 않는다. */
private const val MIN_MEANINGFUL_LENGTH = 4

private const val ELLIPSIS = "…"

/** 소수점은 문장 끝이 아니다("3.5시간 잤어"). 마침표 뒤에 숫자가 오면 경계로 보지 않는다. */
private val SENTENCE_DELIMITER = Regex("""(?:\.(?!\d)|[!?…。！？\n\r])+""")

private val WHITESPACE = Regex("""[\s 　]+""")

/** 어절 끝에 붙어 판정을 방해하는 기호·자모("짜증나ㅠㅠ"). */
private val TRAILING_MARKS = Regex("""[,~\-ㅋㅎㅠㅜ.]+$""")

/** 감탄사·강조어. "개"는 반려견과 부딪혀 넣지 않는다. */
private val INTERJECTIONS = setOf(
    "아", "아아", "어", "음", "하", "하아", "허", "헐", "와", "우와", "에휴", "아오", "아우", "아이고", "휴", "참",
    "진짜", "정말", "너무", "완전", "존나", "매우", "몹시", "엄청", "되게", "그냥", "좀", "막", "약간", "많이",
)

/**
 * 감정 표현 앞에 붙여 쓰는 말("이진짜개짜증나"). 구어체는 띄어쓰기를 잘 지키지 않아
 * 어절 단위 완전 일치만으로는 통째로 놓친다. 벗겨낸 나머지가 감정어일 때만 군더더기로 본다.
 */
private val FILLER_PREFIXES = listOf(
    "이", "그", "저", "아", "어", "하", "진짜", "정말", "너무", "완전", "개", "존나", "핵", "겁나", "졸라", "왕",
)

/**
 * 대화를 연 시점과 겹쳐 방을 구분하지 못하는 말. 제목에서 앞머리면 떼되 사실로 세지 않는다.
 * "내일"·"어제"처럼 다른 날을 가리키는 말은 목록의 생성 일시와 겹치지 않으므로 넣지 않는다.
 */
private val WEAK_WORDS = setOf("오늘", "지금", "방금", "아까", "이제")

/**
 * 감정 표현 어간. 어미와 조합한 완전 일치로만 판정한다.
 * startsWith 로 보면 "괴롭힘"·"행복주택"처럼 제목에 남아야 할 사실 명사까지 삼킨다.
 */
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

/**
 * 어미 없이 단독으로 쓰는 감정 표현. 어간 전부에 빈 어미를 붙이면
 * "불안 장애"·"행복주택"처럼 명사로 쓰인 말까지 삼켜서, 단독형만 따로 적는다.
 */
private val BARE_EMOTIONS = setOf(
    "짜증", "짜증나", "우울", "화나", "화남", "슬퍼", "기뻐", "빡침", "현타", "멘붕", "귀찮", "싫",
)

private val EMOTION_WORDS: Set<String> = EMOTION_STEMS
    .flatMapTo(BARE_EMOTIONS.toMutableSet()) { stem -> EMOTION_ENDINGS.map { stem + it } }

/**
 * 대화를 연 첫 발화(시드)에서 대화방 제목을 만든다. 목록에서 방을 구분하려면 감정 토로가 아니라
 * 무슨 일이 있었는지가 제목에 남아야 하므로, 사실 어절이 있는 문장을 골라 앞머리 군더더기만 뗀다.
 *
 * 요약 모델은 쓰지 않는다. 시드는 140자 이하라 압축할 여지가 적고, 짧은 캐주얼 입력은 요약 모델
 * (긴 문서 학습)의 분포 밖이라 반복·할루시네이션을 만든다.
 *
 * 제목으로 쓸 글자가 없으면 null 이다. 호출자는 제목 지정을 건너뛴다(서버가 빈 제목을 거절한다).
 */
fun conversationTitleFrom(seed: String): String? {
    val sentences = seed.splitSentences()
    if (sentences.isEmpty()) return null

    // 의문문은 도입부이거나 자문이라("대박인거 알려줄까?") 뒤에 오는 사실보다 뒤로 미룬다.
    val (questions, statements) = sentences.filter { titleCandidate(it.text) != null }
        .partition(Sentence::isQuestion)
    val candidates = (statements + questions).mapNotNull { titleCandidate(it.text) }

    // 사실이 짧게 남는 문장에서 멈추면 뒤에 있는 진짜 사실을 놓친다("아 몰라. 팀장이 아이디어 가로챘어").
    val title = candidates.firstOrNull { it.length >= MIN_MEANINGFUL_LENGTH }
        ?: candidates.firstOrNull()
        ?: sentences.map(Sentence::text).headline()
    return title.ellipsize()
}

private class Sentence(val text: String, val isQuestion: Boolean)

/** 종결 부호를 함께 봐야 의문문을 가릴 수 있어 split 대신 경계를 직접 훑는다. */
private fun String.splitSentences(): List<Sentence> {
    val sentences = mutableListOf<Sentence>()
    var start = 0
    for (boundary in SENTENCE_DELIMITER.findAll(this)) {
        sentences += Sentence(substring(start, boundary.range.first), boundary.value.any(Char::isQuestionMark))
        start = boundary.range.last + 1
    }
    sentences += Sentence(substring(start), false)
    return sentences.mapNotNull { sentence ->
        sentence.text.replace(WHITESPACE, " ").trim()
            .takeIf(String::isNotEmpty)
            ?.let { Sentence(it, sentence.isQuestion) }
    }
}

private fun Char.isQuestionMark(): Boolean = this == '?' || this == '？'

/** 사실 어절이 하나도 없는 문장은 제목 후보가 아니다. 문장 중간·뒤는 건드리지 않는다. */
private fun titleCandidate(sentence: String): String? {
    val words = sentence.split(' ')
    if (words.none(::isFactual)) return null
    return words.dropWhile { !isFactual(it) }.joinToString(" ")
}

private fun isFactual(word: String): Boolean {
    val bare = word.replace(TRAILING_MARKS, "")
    if (bare.isEmpty() || bare in WEAK_WORDS) return false
    return !bare.isFiller() && !bare.stripFillerPrefixes().isFiller()
}

private fun String.isFiller(): Boolean = this in INTERJECTIONS || this in EMOTION_WORDS

/**
 * 앞에 붙은 군더더기를 벗겨낸 나머지. "이진짜개짜증나" → "짜증나".
 * 벗긴 결과가 감정어일 때만 군더더기로 판정하므로 "이직"·"개발자"·"괴롭힘"은 그대로 남는다.
 */
private fun String.stripFillerPrefixes(): String {
    var rest = this
    while (true) {
        val prefix = FILLER_PREFIXES.firstOrNull { rest.length > it.length && rest.startsWith(it) } ?: return rest
        rest = rest.removePrefix(prefix)
    }
}

/** 감정과 군더더기뿐인 시드의 폴백. 첫 문장을 쓰되 한 마디면 다음 문장까지 이어 붙인다. */
private fun List<String>.headline(): String {
    var headline = first()
    for (sentence in drop(1)) {
        if (headline.length >= MIN_TITLE_LENGTH) break
        headline = "$headline $sentence"
    }
    return headline
}

private fun String.ellipsize(): String {
    if (length <= MAX_CONVERSATION_TITLE_LENGTH) return this
    val limit = MAX_CONVERSATION_TITLE_LENGTH - ELLIPSIS.length
    // 서로게이트 페어 한가운데를 자르면 깨진 문자가 남는다.
    val end = if (this[limit - 1].isHighSurrogate()) limit - 1 else limit
    return take(end).trimEnd() + ELLIPSIS
}
