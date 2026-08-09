package com.gamss.android.data.repository

import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TokenUsageRefreshNotifierImpl @Inject constructor() : TokenUsageRefreshNotifier {

    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val refreshEvents: Flow<Unit> = events

    override fun requestRefresh() {
        events.tryEmit(Unit)
    }
}
