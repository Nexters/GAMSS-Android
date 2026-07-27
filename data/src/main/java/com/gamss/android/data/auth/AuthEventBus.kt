package com.gamss.android.data.auth

import com.gamss.android.domain.model.AuthEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun notify(event: AuthEvent) {
        _events.tryEmit(event)
    }
}
