package com.gamss.android.data.repository

import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TokenUsageRefreshNotifierImpl @Inject constructor() : TokenUsageRefreshNotifier {

    /** 신호는 마지막 한 번만 의미가 있다. 버퍼가 차면 오래된 쪽을 버려 tryEmit 이 실패하지 않게 한다. */
    private val events = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val refreshEvents: Flow<Unit> = events.asSharedFlow()

    override fun requestRefresh() {
        events.tryEmit(Unit)
    }
}
