package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.calculateTokenUsagePercent
import com.gamss.android.data.di.ApplicationScope
import com.gamss.android.domain.repository.TokenUsageAlert
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** 구독이 늦게 시작돼도 막 emit된 알림 하나는 놓치지 않도록 버퍼 1개만 둔다. */
    private val _alerts = MutableSharedFlow<TokenUsageAlert>(extraBufferCapacity = 1)
    override val alerts: Flow<TokenUsageAlert> = _alerts.asSharedFlow()

    // 화면(ViewModel)을 나갔다 들어와도 안 사라지도록, "이미 알렸는지"는 프로세스가 사는 동안
    // 여기 싱글턴에만 기억한다. LOW는 하루 1회만 알리면 되지만, EXHAUSTED는 전송할 때마다(=
    // refresh() 호출마다) 소진 상태면 매번 알려야 해서 별도 게이팅 플래그가 없다.
    private var hasAlertedLow = false

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
        // 새 하루로 보고 LOW 알림 상태를 초기화한다 — 클라이언트가 리셋 시각을 직접 계산하지 않는다.
        val previousPercent = _usagePercent.value
        val isNewDay = (previousPercent != null && percent != null && percent < previousPercent) ||
            (_isExhausted.value && !usage.exceeded)
        if (isNewDay) {
            hasAlertedLow = false
        }
        _isExhausted.value = usage.exceeded
        _usagePercent.value = percent

        when {
            // exceeded는 서버가 내려주는 확정값이라 반올림 오차가 있는 percent >= 100 보다 정확하다.
            // requestRefresh()가 불릴 때마다(채팅방 진입, 전송 성공 등) 소진 상태면 매번 알린다.
            usage.exceeded -> {
                hasAlertedLow = true
                _alerts.tryEmit(TokenUsageAlert.EXHAUSTED)
            }
            percent != null && percent >= LOW_USAGE_THRESHOLD_PERCENT && !hasAlertedLow -> {
                hasAlertedLow = true
                _alerts.tryEmit(TokenUsageAlert.LOW)
            }
        }
    }

    private companion object {
        const val LOW_USAGE_THRESHOLD_PERCENT = 90
    }
}
