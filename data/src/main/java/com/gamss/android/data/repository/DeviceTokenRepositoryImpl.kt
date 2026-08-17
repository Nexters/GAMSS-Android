package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.push.DeviceTokenService
import com.gamss.android.data.remote.push.model.request.RegisterDeviceTokenRequest
import com.gamss.android.data.remote.push.model.request.UnregisterDeviceTokenRequest
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.push.DeviceTokenRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 세션 인증과 포그라운드 진입이 거의 동시에 동기화를 트리거해 콜드스타트마다 같은 요청이 두 번
 * 나간다. 서버가 멱등이라 깨지지는 않지만 불필요한 호출이라 이미 반영된 상태는 건너뛴다.
 *
 * 실패는 기억하지 않는다. 액세스 토큰 재발급 전에 나간 첫 요청이 401을 받는 경우가 있어,
 * 실패를 반영된 상태로 남기면 다음 트리거가 복구하지 못한다.
 */
@Singleton
internal class DeviceTokenRepositoryImpl @Inject constructor(
    private val deviceTokenService: DeviceTokenService,
) : DeviceTokenRepository {

    private val mutex = Mutex()

    @Volatile
    private var syncedState: SyncedState? = null

    override suspend fun registerToken(token: String): AppResult<Unit> =
        syncOnce(SyncedState(token = token, registered = true)) {
            deviceTokenService.register(RegisterDeviceTokenRequest(token = token))
        }

    override suspend fun unregisterToken(token: String): AppResult<Unit> =
        syncOnce(SyncedState(token = token, registered = false)) {
            deviceTokenService.unregister(UnregisterDeviceTokenRequest(token = token))
        }

    private suspend fun syncOnce(
        target: SyncedState,
        call: suspend () -> ApiResponse<Unit>,
    ): AppResult<Unit> {
        mutex.withLock {
            if (syncedState == target) return AppResult.Success(Unit)
            val result = runCatchingApiCall { call().throwIfFailed() }
            if (result is AppResult.Success) syncedState = target
            return result
        }
    }

    private data class SyncedState(val token: String, val registered: Boolean)
}
