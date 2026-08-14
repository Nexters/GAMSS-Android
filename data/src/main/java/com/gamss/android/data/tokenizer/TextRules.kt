package com.gamss.android.data.tokenizer

/**
 * HuggingFace tokenizers 의 문자 분류 규칙을 옮긴 것.
 * Rust 구현은 유니코드 스칼라 단위로 도는데 Kotlin String 은 UTF-16 이라, 서로게이트 쌍을 쪼개면
 * 이모지 같은 보조 평면 문자가 깨진다. 그래서 이 파일의 함수는 전부 코드포인트를 받는다.
 */

internal const val REPLACEMENT_CODE_POINT = 0xFFFD

/** 서로게이트 쌍을 하나의 코드포인트로 묶어 순회한다. */
internal inline fun forEachCodePoint(text: String, action: (Int) -> Unit) {
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        index += Character.charCount(codePoint)
        action(codePoint)
    }
}

/**
 * 탭·개행·캐리지리턴은 제어문자로 치지 않는다(HuggingFace 와 동일).
 * 나머지는 Cc·Cf·Co·Cs·Cn 카테고리를 제어문자로 본다.
 */
internal fun isControlCodePoint(codePoint: Int): Boolean {
    if (codePoint == '\t'.code || codePoint == '\n'.code || codePoint == '\r'.code) return false
    return when (Character.getType(codePoint)) {
        Character.CONTROL.toInt(),
        Character.FORMAT.toInt(),
        Character.PRIVATE_USE.toInt(),
        Character.SURROGATE.toInt(),
        Character.UNASSIGNED.toInt(),
        -> true

        else -> false
    }
}

/**
 * 유니코드 White_Space 속성. Java 의 Character.isWhitespace 는 U+00A0 같은 것을 빼서
 * Rust char::is_whitespace 와 결과가 달라지므로 직접 나열한다.
 */
internal fun isUnicodeWhitespace(codePoint: Int): Boolean = when (codePoint) {
    in 0x09..0x0D, 0x20, 0x85, 0xA0, 0x1680, in 0x2000..0x200A,
    0x2028, 0x2029, 0x202F, 0x205F, 0x3000,
    -> true

    else -> false
}

/** BertNormalizer 가 앞뒤에 공백을 넣어 한 글자씩 떼어내는 CJK 범위. */
internal fun isChineseChar(codePoint: Int): Boolean = when (codePoint) {
    in 0x4E00..0x9FFF, in 0x3400..0x4DBF, in 0x20000..0x2A6DF, in 0x2A700..0x2B73F,
    in 0x2B740..0x2B81F, in 0x2B920..0x2CEAF, in 0xF900..0xFAFF, in 0x2F800..0x2FA1F,
    -> true

    else -> false
}

/** ASCII 구두점 전체 + 유니코드 P* 카테고리. BertPreTokenizer 가 한 글자씩 분리하는 대상이다. */
internal fun isBertPunctuation(codePoint: Int): Boolean {
    val isAsciiPunctuation = codePoint in 0x21..0x2F ||
        codePoint in 0x3A..0x40 ||
        codePoint in 0x5B..0x60 ||
        codePoint in 0x7B..0x7E
    if (isAsciiPunctuation) return true
    return when (Character.getType(codePoint)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        -> true

        else -> false
    }
}
