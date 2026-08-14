package com.gamss.android.data.tokenizer

/**
 * HuggingFace tokenizers 의 문자 분류 규칙 이식.
 * 서로게이트 쌍을 쪼개면 이모지가 깨지므로 이 파일의 함수는 전부 코드포인트를 받는다.
 */

internal const val REPLACEMENT_CODE_POINT = 0xFFFD

internal inline fun forEachCodePoint(text: String, action: (Int) -> Unit) {
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        index += Character.charCount(codePoint)
        action(codePoint)
    }
}

/** 탭·개행·캐리지리턴은 제어문자로 치지 않는다(HuggingFace 와 동일). */
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

/** Character.isWhitespace 는 U+00A0 등을 빼서 Rust 와 결과가 달라지므로 직접 나열한다. */
internal fun isUnicodeWhitespace(codePoint: Int): Boolean = when (codePoint) {
    in 0x09..0x0D, 0x20, 0x85, 0xA0, 0x1680, in 0x2000..0x200A,
    0x2028, 0x2029, 0x202F, 0x205F, 0x3000,
    -> true

    else -> false
}

/** BertNormalizer 가 한 글자씩 떼어내는 CJK 범위. */
internal fun isChineseChar(codePoint: Int): Boolean = when (codePoint) {
    in 0x4E00..0x9FFF, in 0x3400..0x4DBF, in 0x20000..0x2A6DF, in 0x2A700..0x2B73F,
    in 0x2B740..0x2B81F, in 0x2B920..0x2CEAF, in 0xF900..0xFAFF, in 0x2F800..0x2FA1F,
    -> true

    else -> false
}

/** ASCII 구두점 전체 + 유니코드 P* 카테고리. */
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
