package com.gamss.android.domain.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageContentPolicyTest {

    @Test
    fun 한도_이하면_그대로_둔다() {
        val text = "가".repeat(MAX_MESSAGE_LENGTH - 1)

        assertEquals(text, text.takeWithinMessageLimit())
    }

    @Test
    fun 한도를_넘으면_경계의_이모지가_절반만_남지_않는다() {
        val head = "가".repeat(MAX_MESSAGE_LENGTH - 1)

        assertEquals(head, (head + GRINNING_FACE).takeWithinMessageLimit())
    }

    @Test
    fun 한도에_정확히_들어맞는_이모지는_보존된다() {
        val text = "가".repeat(MAX_MESSAGE_LENGTH - 2) + GRINNING_FACE

        assertEquals(text, text.takeWithinMessageLimit())
    }

    @Test
    fun 경계의_변형_선택자_이모지는_함께_잘린다() {
        val head = "가".repeat(MAX_MESSAGE_LENGTH - 1)

        assertEquals(head, (head + RED_HEART).takeWithinMessageLimit())
    }

    @Test
    fun 경계의_결합_이모지는_절반만_남지_않는다() {
        val head = "가".repeat(MAX_MESSAGE_LENGTH - 4)

        assertEquals(head, (head + WOMAN_TECHNOLOGIST).takeWithinMessageLimit())
    }

    @Test
    fun 결합이_여러_번인_이모지도_통째로_잘린다() {
        val head = "가".repeat(MAX_MESSAGE_LENGTH - 6)

        assertEquals(head, (head + FAMILY + "가").takeWithinMessageLimit())
    }

    @Test
    fun 한도_안에_들어오는_결합_이모지는_뒤가_잘려도_보존된다() {
        val kept = "가".repeat(MAX_MESSAGE_LENGTH - 5) + WOMAN_TECHNOLOGIST

        assertEquals(kept, (kept + "가".repeat(10)).takeWithinMessageLimit())
    }

    @Test
    fun 한도가_0_이하면_빈_문자열이_된다() {
        assertEquals("", "가나다".takeWithinMessageLimit(maxLength = 0))
    }

    private companion object {
        const val GRINNING_FACE = "\uD83D\uDE00"
        const val RED_HEART = "\u2764\uFE0F"
        const val WOMAN_TECHNOLOGIST = "\uD83D\uDC69\u200D\uD83D\uDCBB"
        const val FAMILY = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"
    }
}
