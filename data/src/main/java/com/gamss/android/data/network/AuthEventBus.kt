package com.gamss.android.data.network

import com.gamss.android.domain.model.AuthEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

internal interface AuthEventBus {
    val events: SharedFlow<AuthEvent>

    fun notify(event: AuthEvent)
}

@Singleton
internal class AuthEventBusImpl @Inject constructor() : AuthEventBus {

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    override fun notify(event: AuthEvent) {
        _events.tryEmit(event)
    }
}
