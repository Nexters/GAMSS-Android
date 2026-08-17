package com.gamss.android.domain.push

interface PushTokenProvider {

    /** SDK 조회가 실패하면 null. 예외를 던지지 않는다. */
    suspend fun getToken(): String?
}
