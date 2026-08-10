package com.gamss.android.app.main

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.auth.ObserveSessionStateUseCase
import com.gamss.android.domain.auth.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val observeSessionStateUseCase: ObserveSessionStateUseCase,
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
        observeSessionStateUseCase().collect { sessionState ->
            reduce { sessionState }
        }
    }
}
