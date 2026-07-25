package com.gamss.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.usecase.ObserveAuthEventsUseCase
import com.gamss.android.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object Authenticated : SessionState
    data object Unauthenticated : SessionState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    restoreSessionUseCase: RestoreSessionUseCase,
    observeAuthEventsUseCase: ObserveAuthEventsUseCase,
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _shouldNavigateToLogin = MutableStateFlow(false)
    val shouldNavigateToLogin: StateFlow<Boolean> = _shouldNavigateToLogin.asStateFlow()

    init {
        viewModelScope.launch {
            _sessionState.value = when (restoreSessionUseCase()) {
                is AppResult.Success -> SessionState.Authenticated
                is AppResult.Failure -> SessionState.Unauthenticated
            }
        }

        viewModelScope.launch {
            observeAuthEventsUseCase().collect { event ->
                when (event) {
                    is AuthEvent.SessionExpired, is AuthEvent.LoggedOut -> {
                        _shouldNavigateToLogin.value = true
                    }
                }
            }
        }
    }

    fun onNavigatedToLogin() {
        _shouldNavigateToLogin.value = false
    }
}
