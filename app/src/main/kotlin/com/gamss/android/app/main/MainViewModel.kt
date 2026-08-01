package com.gamss.android.app.main

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.usecase.ObserveAuthEventsUseCase
import com.gamss.android.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object Authenticated : SessionState
    data object Unauthenticated : SessionState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val observeAuthEventsUseCase: ObserveAuthEventsUseCase,
) : ViewModel(), ContainerHost<SessionState, Unit> {

    override val container = container<SessionState, Unit>(SessionState.Loading)

    init {
        restoreSession()
        observeAuthEvents()
    }

    private fun restoreSession() = intent {
        val result = restoreSessionUseCase()
        reduce {
            when (result) {
                is AppResult.Success -> {
                    if (result.data) {
                        SessionState.Authenticated
                    } else {
                        SessionState.Unauthenticated
                    }
                }
                is AppResult.Failure -> SessionState.Unauthenticated
            }
        }
    }

    private fun observeAuthEvents() = intent {
        observeAuthEventsUseCase().collect { event ->
            when (event) {
                is AuthEvent.SessionExpired, is AuthEvent.LoggedOut -> {
                    reduce { SessionState.Unauthenticated }
                }
            }
        }
    }

    fun onLoginSucceeded() = intent {
        reduce { SessionState.Authenticated }
    }
}
