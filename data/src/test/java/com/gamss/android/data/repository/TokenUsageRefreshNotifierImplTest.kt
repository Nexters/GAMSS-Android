package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.repository.TokenUsageAlert
import com.gamss.android.domain.user.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenUsageRefreshNotifierImplTest {

    private val userRepository: UserRepository = mockk()

    @Test
    fun `소진 상태면 요청할 때마다 EXHAUSTED를 매번 흘려보낸다`() = runTest {
        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 100, dailyLimit = 100, exceeded = true))
        val notifier = TokenUsageRefreshNotifierImpl(userRepository, backgroundScope)
        val received = collectAlerts(notifier)

        repeat(3) {
            notifier.requestRefresh()
            advanceUntilIdle()
        }

        assertEquals(
            listOf(TokenUsageAlert.EXHAUSTED, TokenUsageAlert.EXHAUSTED, TokenUsageAlert.EXHAUSTED),
            received,
        )
    }

    @Test
    fun `10퍼센트 미만 남음은 같은 날엔 한 번만 흘려보낸다`() = runTest {
        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 95, dailyLimit = 100, exceeded = false))
        val notifier = TokenUsageRefreshNotifierImpl(userRepository, backgroundScope)
        val received = collectAlerts(notifier)

        repeat(3) {
            notifier.requestRefresh()
            advanceUntilIdle()
        }

        assertEquals(listOf(TokenUsageAlert.LOW), received)
    }

    @Test
    fun `소진 이후 사용량이 리셋되면 10퍼센트 미만 남음 알림이 다시 발행된다`() = runTest {
        val notifier = TokenUsageRefreshNotifierImpl(userRepository, backgroundScope)
        val received = collectAlerts(notifier)

        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 100, dailyLimit = 100, exceeded = true))
        notifier.requestRefresh()
        advanceUntilIdle()

        // 자정/오전 5시 리셋으로 사용량이 다시 낮아졌다가 90%대로 재진입한 상황.
        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 0, dailyLimit = 100, exceeded = false))
        notifier.requestRefresh()
        advanceUntilIdle()

        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 92, dailyLimit = 100, exceeded = false))
        notifier.requestRefresh()
        advanceUntilIdle()

        assertEquals(listOf(TokenUsageAlert.EXHAUSTED, TokenUsageAlert.LOW), received)
    }

    /**
     * [notifier.alerts]는 replay 없는 SharedFlow라, 구독이 emit보다 늦으면 그 알림을 영영 놓친다.
     * UNDISPATCHED로 즉시 구독까지 실행해 이후의 requestRefresh() 호출보다 항상 먼저 구독되도록
     * 보장한다. 무한 수집이라 backgroundScope에 태워 테스트 종료 시 자동으로 취소되게 한다.
     *
     * [TokenUsageRefreshNotifierImpl]의 init도 내부적으로 refreshEvents를 구독하는 코루틴을
     * (UNDISPATCHED가 아닌 일반 launch로) 띄운다. 이 구독 역시 같은 replay=0 문제를 겪으므로,
     * 여기서 advanceUntilIdle()을 한 번 태워 그 구독이 실제로 걸리게 한 뒤에 반환해야
     * 테스트에서의 첫 requestRefresh() 신호를 놓치지 않는다.
     */
    private fun TestScope.collectAlerts(
        notifier: TokenUsageRefreshNotifierImpl,
    ): MutableList<TokenUsageAlert> {
        val received = mutableListOf<TokenUsageAlert>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            notifier.alerts.toList(received)
        }
        advanceUntilIdle()
        return received
    }

    private fun usage(usedTokens: Long, dailyLimit: Long?, exceeded: Boolean) = DailyTokenUsage(
        usedTokens = usedTokens,
        dailyLimit = dailyLimit,
        exceeded = exceeded,
    )
}
