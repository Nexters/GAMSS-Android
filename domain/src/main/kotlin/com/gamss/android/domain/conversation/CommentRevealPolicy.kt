package com.gamss.android.domain.conversation

import kotlin.random.Random

const val COMMENT_REVEAL_MIN_GAP_MILLIS = 500L
const val COMMENT_REVEAL_MAX_GAP_MILLIS = 3_000L

fun nextCommentRevealGapMillis(random: Random = Random.Default): Long =
    random.nextLong(COMMENT_REVEAL_MIN_GAP_MILLIS, COMMENT_REVEAL_MAX_GAP_MILLIS + 1)
