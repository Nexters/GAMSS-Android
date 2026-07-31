package com.gamss.android.data.repository

import retrofit2.HttpException

/**
 * 서버는 "이미 종료됨", "이미 카드 있음"을 409 로 알려준다. 이걸 그냥 실패로 흘리면
 * 서버는 성공했는데 클라만 실패로 본 경우(타임아웃 등) 재시도가 영구히 409 라서 복구가 막힌다.
 */
internal fun Throwable.isConflict(): Boolean = this is HttpException && code() == HTTP_CONFLICT

private const val HTTP_CONFLICT = 409
