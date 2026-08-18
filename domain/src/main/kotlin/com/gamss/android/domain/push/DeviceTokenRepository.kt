package com.gamss.android.domain.push

import com.gamss.android.core.common.AppResult

interface DeviceTokenRepository {

    suspend fun registerToken(token: String): AppResult<Unit>

    suspend fun unregisterToken(token: String): AppResult<Unit>
}
