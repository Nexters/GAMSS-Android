package com.gamss.android.domain.conversation

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CommentRevealPolicyTest {

    @Test
    fun 간격은_항상_정책_범위_안이다() {
        val random = Random(SEED)

        repeat(SAMPLES) {
            val gap = CommentRevealPolicy.nextGapMillis(random)
            assertTrue(
                "gap=$gap",
                gap in CommentRevealPolicy.MIN_GAP_MILLIS..CommentRevealPolicy.MAX_GAP_MILLIS,
            )
        }
    }

    private companion object {
        const val SEED = 42
        const val SAMPLES = 2_000
    }
}
