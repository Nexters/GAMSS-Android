package com.gamss.android.app.main

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.auth.ObserveSessionStateUseCase
import com.gamss.android.domain.auth.RestoreSessionUseCase
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.config.GetRemoteConfigFlagUseCase
import com.gamss.android.domain.config.ObserveRemoteConfigReadyUseCase
import com.gamss.android.domain.config.RemoteConfigKey
import com.gamss.android.domain.push.IsNotificationPermissionGrantedUseCase
import com.gamss.android.domain.push.SyncDeviceTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val observeSessionStateUseCase: ObserveSessionStateUseCase,
    private val observeRemoteConfigReadyUseCase: ObserveRemoteConfigReadyUseCase,
    private val getRemoteConfigFlagUseCase: GetRemoteConfigFlagUseCase,
    private val syncDeviceTokenUseCase: SyncDeviceTokenUseCase,
    private val isNotificationPermissionGrantedUseCase: IsNotificationPermissionGrantedUseCase,
) : ViewModel(), ContainerHost<MainState, Unit> {

    override val container = container<MainState, Unit>(MainState())

    init {
        restoreSession()
        observeSessionState()
    }

    private fun restoreSession() = intent {
        restoreSessionUseCase()
    }

    private fun observeSessionState() = intent {
        combine(observeSessionStateUseCase(), remoteConfigGate()) { sessionState, useCardFeature ->
            sessionState to useCardFeature
        }.collect { (sessionState, useCardFeature) ->
            reduce { state.copy(sessionState = sessionState, useCardFeature = useCardFeature) }
            syncDeviceTokenIfAuthenticated(sessionState)
        }
    }

    fun syncDeviceToken() = intent {
        syncDeviceTokenIfAuthenticated(state.sessionState)
    }

    suspend fun isNotificationPermissionGranted(): Boolean = isNotificationPermissionGrantedUseCase()

    private suspend fun syncDeviceTokenIfAuthenticated(sessionState: SessionState) {
        if (sessionState == SessionState.Authenticated) syncDeviceTokenUseCase()
    }

    /**
     * 원격 설정이 늦어지면 콜드스타트를 붙잡지 않고 기본값으로 먼저 진행한다.
     *
     * 타임아웃으로 먼저 내보낸 경우에는 준비가 끝난 뒤 한 번 더 읽는다. 그러지 않으면 느린 콜드스타트에서
     * 기본값이 앱을 다시 켤 때까지 굳는다.
     */
    private fun remoteConfigGate(): Flow<Boolean> = flow {
        val readyInTime = withTimeoutOrNull(REMOTE_CONFIG_WAIT_MILLIS) {
            observeRemoteConfigReadyUseCase().first { isReady -> isReady }
        } != null
        emit(getRemoteConfigFlagUseCase(RemoteConfigKey.UseChatEndFeature))
        if (!readyInTime) {
            observeRemoteConfigReadyUseCase().first { isReady -> isReady }
            emit(getRemoteConfigFlagUseCase(RemoteConfigKey.UseChatEndFeature))
        }
    }

    private companion object {
        val REMOTE_CONFIG_WAIT_MILLIS = 3.seconds.inWholeMilliseconds
    }
}
