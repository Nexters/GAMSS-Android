package com.gamss.android.domain.conversation

import kotlin.random.Random

/**
 * 캐릭터 댓글을 한꺼번에 띄우지 않고 하나씩 노출할 때의 간격.
 *
 * 첫 댓글은 서버 왕복이 곧 대기 시간이라 즉시 띄우고, 두 번째부터 이 간격을 둔다.
 * 지연은 표시 타이밍이라 대화 복원(메시지 조회)에는 적용하지 않는다.
 */
const val COMMENT_REVEAL_MIN_GAP_MILLIS = 1_000L
const val COMMENT_REVEAL_MAX_GAP_MILLIS = 3_000L

fun nextCommentRevealGapMillis(random: Random = Random.Default): Long =
    random.nextLong(COMMENT_REVEAL_MIN_GAP_MILLIS, COMMENT_REVEAL_MAX_GAP_MILLIS + 1)
