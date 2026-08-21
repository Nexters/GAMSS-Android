package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.calculateTokenUsagePercent
import com.gamss.android.data.di.ApplicationScope
import com.gamss.android.domain.repository.TokenUsageAlert
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TokenUsageRefreshNotifierImpl @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : TokenUsageRefreshNotifier {

    /** 신호는 마지막 한 번만 의미가 있다. 버퍼가 차면 오래된 쪽을 버려 tryEmit 이 실패하지 않게 한다. */
    private val events = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val refreshEvents: Flow<Unit> = events.asSharedFlow()

    private val _usagePercent = MutableStateFlow<Int?>(null)
    override val usagePercent: StateFlow<Int?> = _usagePercent.asStateFlow()

    private val _isExhausted = MutableStateFlow(false)
    override val isExhausted: StateFlow<Boolean> = _isExhausted.asStateFlow()

    /** 알림은 하루/타입당 정확히 1번만 소비돼야 해서, 재생되는 Channel을 쓴다. */
    private val _alerts = Channel<TokenUsageAlert>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val alerts: Flow<TokenUsageAlert> = _alerts.receiveAsFlow()

    // 화면(ViewModel)을 나갔다 들어오거나 다른 채팅방에 진입해도 중복으로 안 뜨도록, "이미
    // 알렸는지"는 프로세스가 사는 동안 여기 싱글턴에만 기억한다. isExhausted는 이 플래그와
    // 무관하게 refresh() 때마다 항상 최신화된다 — 알림은 한 번만, 상태 반영(입력창 잠금)은
    // 매번.
    private var hasAlertedLow = false
    private var hasAlertedExhausted = false

    init {
        applicationScope.launch {
            refreshEvents.collect { refresh() }
        }
    }

    override fun requestRefresh() {
        events.tryEmit(Unit)
    }

    private suspend fun refresh() {
        val result = userRepository.getDailyTokenUsage()
        if (result !is AppResult.Success) return
        val usage = result.data
        val percent = calculateTokenUsagePercent(usage.usedTokens, usage.dailyLimit)

        // 이전보다 사용량이 줄었거나(서버가 오전 5시에 리셋) 더 이상 소진 상태가 아니라면
        // 새 하루로 보고 알림 상태를 초기화한다 — 클라이언트가 리셋 시각을 직접 계산하지 않는다.
        val previousPercent = _usagePercent.value
        val isNewDay = (previousPercent != null && percent != null && percent < previousPercent) ||
            (_isExhausted.value && !usage.exceeded)
        if (isNewDay) {
            hasAlertedLow = false
            hasAlertedExhausted = false
        }
        _isExhausted.value = usage.exceeded
        _usagePercent.value = percent

        when {
            // exceeded는 서버가 내려주는 확정값이라 반올림 오차가 있는 percent >= 100 보다 정확하다.
            // 알림은 하루 1회만 — 소진시킨 그 전송 시점에만 뜨고, 이후 다른 채팅방에 들어가거나
            // 같은 방에 재진입해 refresh()가 다시 돌아도 다시 뜨지 않는다. isExhausted 자체는
            // 이 게이팅과 무관하게 위에서 매번 최신화되므로 입력창 잠금은 계속 정확하다.
            usage.exceeded && !hasAlertedExhausted -> {
                hasAlertedExhausted = true
                hasAlertedLow = true
                _alerts.trySend(TokenUsageAlert.EXHAUSTED)
            }
            percent != null && percent >= LOW_USAGE_THRESHOLD_PERCENT && !hasAlertedLow -> {
                hasAlertedLow = true
                _alerts.trySend(TokenUsageAlert.LOW)
            }
        }
    }

    private companion object {
        const val LOW_USAGE_THRESHOLD_PERCENT = 90
    }
}
