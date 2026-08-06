package com.gamss.android.domain.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CommentRevealPolicyTest {

    private class CapturingRandom : Random() {
        var capturedUntil: Long? = null
            private set

        override fun nextBits(bitCount: Int): Int = 0

        override fun nextLong(from: Long, until: Long): Long {
            capturedUntil = until
            return from
        }
    }

    @Test
    fun 상한을_포함하도록_exclusive_경계에_하나를_더한다() {
        val random = CapturingRandom()

        nextCommentRevealGapMillis(random)

        assertEquals(COMMENT_REVEAL_MAX_GAP_MILLIS + 1, random.capturedUntil)
    }

    @Test
    fun 간격은_항상_정책_범위_안이다() {
        val random = Random(SEED)

        repeat(SAMPLES) {
            val gap = nextCommentRevealGapMillis(random)
            assertTrue("gap=$gap", gap in COMMENT_REVEAL_MIN_GAP_MILLIS..COMMENT_REVEAL_MAX_GAP_MILLIS)
        }
    }

    private companion object {
        const val SEED = 42
        const val SAMPLES = 2_000
    }
}
