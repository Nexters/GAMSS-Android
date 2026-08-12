package com.gamss.android.data.repository

import com.gamss.android.core.common.network.ApiException

/**
 * 서버가 준 error.code 로 분기한다. 이 코드들을 그냥 실패로 흘리면, 서버는 성공했는데 클라만
 * 실패로 본 경우(타임아웃 등) 재시도가 영구히 같은 오류라서 복구가 막힌다.
 */
internal fun Throwable.hasErrorCode(code: String): Boolean =
    this is ApiException && this.code == code

internal const val CONVERSATION_ALREADY_ENDED = "CONVERSATION_ALREADY_ENDED"
internal const val CONVERSATION_ALREADY_DELETED = "CONVERSATION_ALREADY_DELETED"
internal const val CARD_ALREADY_EXISTS = "CARD_ALREADY_EXISTS"
