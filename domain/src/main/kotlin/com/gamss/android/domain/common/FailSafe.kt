package com.gamss.android.domain.common

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * 온디바이스 모델 호출이 죽어도 기능을 막지 않는다. 취소는 삼키지 않는다.
 *
 * 한 곳에 모아 둔다. 복사본에서 [ensureActive] 를 빠뜨리면 취소가 성공처럼 보여 조용히 틀린다.
 */
internal suspend fun <T> failSafe(block: suspend () -> T): T? {
    val result = runCatching { block() }
    currentCoroutineContext().ensureActive()
    return result.getOrNull()
}
