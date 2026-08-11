package com.gamss.android.domain.conversation

import java.text.BreakIterator
import java.util.Locale

const val MAX_MESSAGE_LENGTH = 140

private const val ZERO_WIDTH_JOINER = '\u200D'

/**
 * 길이는 UTF-16 단위로 센다. 경계에 걸린 글자는 반쪽만 남기지 않고 통째로 버린다.
 *
 * ZWJ 결합 이모지는 BreakIterator 가 묶어주지 않아 직접 보정한다. ICU 62 이하(API 28 이하)는
 * ZWJ 뒤에서 끊고, 호스트 JVM 구현은 ZWJ 를 독립 클러스터로 두어 앞뒤 모두에서 끊는다.
 * 그래서 경계의 양쪽을 본다.
 *
 * 국기와 피부색 수정자는 보정하지 않는다. 지원하는 모든 API 의 ICU 가 한 글자로 묶는다.
 * 호스트 JVM 구현은 이들도 끊으므로 해당 경계 테스트는 기기 동작과 다르다.
 */
fun String.takeWithinMessageLimit(maxLength: Int = MAX_MESSAGE_LENGTH): String {
    val endExclusive = when {
        maxLength <= 0 -> 0
        length <= maxLength -> length
        else -> {
            val breaks = BreakIterator.getCharacterInstance(Locale.ROOT)
                .apply { setText(this@takeWithinMessageLimit) }
            var end = breaks.boundaryBefore(maxLength + 1)
            while (isZwjBoundary(end)) {
                end = breaks.boundaryBefore(end)
            }
            end
        }
    }
    return substring(0, endExclusive)
}

private fun BreakIterator.boundaryBefore(offset: Int): Int =
    preceding(offset).takeIf { it != BreakIterator.DONE } ?: 0

private fun String.isZwjBoundary(boundary: Int): Boolean =
    boundary in 1..length &&
        (this[boundary - 1] == ZERO_WIDTH_JOINER || (boundary < length && this[boundary] == ZERO_WIDTH_JOINER))
