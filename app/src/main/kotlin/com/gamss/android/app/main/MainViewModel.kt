package com.gamss.android.app.main

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.auth.ObserveSessionStateUseCase
import com.gamss.android.domain.auth.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val observeSessionStateUseCase: ObserveSessionStateUseCase,
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
        }
    }

    /** 카드 기능은 현재 원격 설정과 무관하게 항상 노출한다. */
    private fun remoteConfigGate(): Flow<Boolean> = flow {
        emit(true)
    }
}
