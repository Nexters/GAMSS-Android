package com.gamss.android.domain.config

import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {

    /** 원격 조회 실패도 준비 완료로 본다. */
    val isReady: Flow<Boolean>

    /** 예외를 던지지 않는다. 성공 여부와 무관하게 원격 조회는 한 번만 수행한다. */
    suspend fun initialize()

    /**
     * 원격 값이 없으면 [RemoteConfigKey.defaultValue] 를 돌려준다.
     * [initialize] 가 떠 둔 스냅샷만 읽으므로 예외를 던지지 않고 블로킹하지 않는다.
     */
    fun getString(key: RemoteConfigKey): String

    fun getBoolean(key: RemoteConfigKey): Boolean
}
