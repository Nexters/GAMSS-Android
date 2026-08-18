package com.gamss.android.app.main

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.auth.ObserveSessionStateUseCase
import com.gamss.android.domain.auth.RestoreSessionUseCase
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.push.MarkNotificationPermissionPromptedUseCase
import com.gamss.android.domain.push.ShouldPromptNotificationPermissionUseCase
import com.gamss.android.domain.push.SyncDeviceTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val observeSessionStateUseCase: ObserveSessionStateUseCase,
    private val syncDeviceTokenUseCase: SyncDeviceTokenUseCase,
    private val shouldPromptNotificationPermissionUseCase: ShouldPromptNotificationPermissionUseCase,
    private val markNotificationPermissionPromptedUseCase: MarkNotificationPermissionPromptedUseCase,
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
        observeSessionStateUseCase().collect { sessionState ->
            reduce { state.copy(sessionState = sessionState) }
            syncDeviceTokenIfAuthenticated(sessionState)
        }
    }

    fun syncDeviceToken() = intent {
        syncDeviceTokenIfAuthenticated(state.sessionState)
    }

    suspend fun shouldPromptNotificationPermission(): Boolean = shouldPromptNotificationPermissionUseCase()

    fun markNotificationPermissionPrompted() = intent {
        markNotificationPermissionPromptedUseCase()
    }

    private suspend fun syncDeviceTokenIfAuthenticated(sessionState: SessionState) {
        if (sessionState == SessionState.Authenticated) syncDeviceTokenUseCase()
    }
}
