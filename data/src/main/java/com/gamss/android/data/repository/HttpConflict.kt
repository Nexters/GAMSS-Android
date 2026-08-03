package com.gamss.android.data.repository

import com.gamss.android.core.common.network.ApiException

/**
 * 서버는 "이미 종료됨", "이미 카드 있음"을 409 + error.code 로 알려준다. 이걸 그냥 실패로 흘리면
 * 서버는 성공했는데 클라만 실패로 본 경우(타임아웃 등) 재시도가 영구히 409 라서 복구가 막힌다.
 */
internal fun Throwable.hasErrorCode(code: String): Boolean =
    this is ApiException && this.code == code

internal const val CONVERSATION_ALREADY_ENDED = "CONVERSATION_ALREADY_ENDED"
internal const val CARD_ALREADY_EXISTS = "CARD_ALREADY_EXISTS"
