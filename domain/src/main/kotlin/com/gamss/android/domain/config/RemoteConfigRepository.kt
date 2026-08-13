package com.gamss.android.domain.config

import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {

    /** 원격 조회 실패도 준비 완료로 본다. */
    val isReady: Flow<Boolean>

    /** 예외를 던지지 않는다. 두 번 이상 호출해도 원격 조회는 한 번만 수행한다. */
    suspend fun initialize()

    /** 원격 값이 없으면 [RemoteConfigKey.defaultValue] 를 돌려준다. */
    fun getString(key: RemoteConfigKey): String

    fun getBoolean(key: RemoteConfigKey): Boolean
}
