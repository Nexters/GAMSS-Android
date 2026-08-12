package com.gamss.android.domain.auth

/**
 * refreshToken이 없거나 서버가 거부해 세션을 더 이상 유지할 수 없을 때 발생한다.
 * 이 예외를 받은 쪽은 로그아웃 처리 후 로그인 화면으로 이동시켜야 한다.
 */
class SessionExpiredException(cause: Throwable? = null) : RuntimeException(
    "Session expired: refresh token is missing or rejected",
    cause,
)
