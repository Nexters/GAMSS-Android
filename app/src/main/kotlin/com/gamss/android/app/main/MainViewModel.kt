package com.gamss.android.app.main

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.auth.ObserveSessionStateUseCase
import com.gamss.android.domain.auth.RestoreSessionUseCase
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.config.ObserveRemoteConfigReadyUseCase
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
) : ViewModel(), ContainerHost<SessionState, Unit> {

    override val container = container<SessionState, Unit>(SessionState.Loading)

    init {
        restoreSession()
        observeSessionState()
    }

    private fun restoreSession() = intent {
        restoreSessionUseCase()
    }

    private fun observeSessionState() = intent {
        combine(observeSessionStateUseCase(), remoteConfigGate()) { sessionState, _ -> sessionState }
            .collect { sessionState -> reduce { sessionState } }
    }

    /** 원격 설정이 늦어지면 콜드스타트를 붙잡지 않고 캐시나 기본값으로 진행한다. */
    private fun remoteConfigGate(): Flow<Unit> = flow {
        withTimeoutOrNull(REMOTE_CONFIG_WAIT_MILLIS) {
            observeRemoteConfigReadyUseCase().first { isReady -> isReady }
        }
        emit(Unit)
    }

    private companion object {
        val REMOTE_CONFIG_WAIT_MILLIS = 3.seconds.inWholeMilliseconds
    }
}
