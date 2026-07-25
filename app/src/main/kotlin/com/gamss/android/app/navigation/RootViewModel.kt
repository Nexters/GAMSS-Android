package com.gamss.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.usecase.ObserveAuthEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    observeAuthEventsUseCase: ObserveAuthEventsUseCase,
) : ViewModel() {

    private val _shouldNavigateToLogin = MutableStateFlow(false)
    val shouldNavigateToLogin: StateFlow<Boolean> = _shouldNavigateToLogin.asStateFlow()

    init {
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
