package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.repository.TokenUsageAlert
import com.gamss.android.domain.user.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TokenUsageRefreshNotifierImplTest {

    private val userRepository: UserRepository = mockk()

    @Test
    fun `소진 상태면 같은 날엔 EXHAUSTED를 한 번만 흘려보낸다`() = runTest {
        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 100, dailyLimit = 100, exceeded = true))
        val notifier = newNotifier()
        val received = collectAlerts(notifier)

        // 소진시킨 전송 직후 1회 + 이후 다른 채팅방 진입 등으로 재조회되는 경우를 흉내낸다.
        repeat(3) {
            notifier.requestRefresh()
            advanceUntilIdle()
        }

        assertEquals(listOf(TokenUsageAlert.EXHAUSTED), received)
    }

    @Test
    fun `10퍼센트 미만 남음은 같은 날엔 한 번만 흘려보낸다`() = runTest {
        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 95, dailyLimit = 100, exceeded = false))
        val notifier = newNotifier()
        val received = collectAlerts(notifier)

        repeat(3) {
            notifier.requestRefresh()
            advanceUntilIdle()
        }

        assertEquals(listOf(TokenUsageAlert.LOW), received)
    }

    @Test
    fun `소진 이후 사용량이 리셋되면 10퍼센트 미만 남음 알림이 다시 발행된다`() = runTest {
        val notifier = newNotifier()
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

    @Test
    fun `isExhausted는 소진 여부를 그대로 반영하고 리셋되면 다시 false가 된다`() = runTest {
        val notifier = newNotifier()
        assertEquals(false, notifier.isExhausted.value)

        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 100, dailyLimit = 100, exceeded = true))
        notifier.requestRefresh()
        advanceUntilIdle()
        assertEquals(true, notifier.isExhausted.value)

        // 자정/오전 5시 리셋으로 더 이상 소진 상태가 아니게 된 상황.
        coEvery { userRepository.getDailyTokenUsage() } returns
            AppResult.Success(usage(usedTokens = 0, dailyLimit = 100, exceeded = false))
        notifier.requestRefresh()
        advanceUntilIdle()
        assertEquals(false, notifier.isExhausted.value)
    }

    /**
     * [TokenUsageRefreshNotifierImpl]의 `init`이 내부적으로 `refreshEvents`를 구독하는 코루틴을
     * 일반 `launch`(UNDISPATCHED가 아닌)로 띄운다. 이 프로젝트의 `TestScope.backgroundScope`
     * (StandardTestDispatcher)에서는 그런 일반 `launch`가 `advanceUntilIdle()`로 실행 큐에
     * 올라오지 않는 채로 남는 문제가 있어 — 실제로 원인을 좁혀보면, launch 시작 시점에는
     * 스케줄러에 태스크가 등록되지만 그 뒤로 전혀 드레인되지 않는다 — 여기선 그 문제를 피하려고
     * `applicationScope`를 `UnconfinedTestDispatcher`로 구성한다. Unconfined 코루틴은 `launch()`
     * 호출 시점에 첫 중단점까지 즉시(같은 스레드에서) 실행되므로, `advanceUntilIdle()`에 기대지
     * 않고도 `refreshEvents.collect { refresh() }` 구독이 노티파이어 생성 즉시 걸린다.
     */
    private fun TestScope.newNotifier(): TokenUsageRefreshNotifierImpl =
        TokenUsageRefreshNotifierImpl(userRepository, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

    /**
     * [notifier.alerts]는 replay 없는 SharedFlow라, 구독이 emit보다 늦으면 그 알림을 영영 놓친다.
     * 구독을 `UnconfinedTestDispatcher`로 돌려 subscribe든 emit 이후의 재개든 모두 즉시(같은
     * 스레드에서) 일어나게 한다 — `backgroundScope`의 기본 `StandardTestDispatcher`로 구독을
     * 걸면, 최초 구독(UNDISPATCHED 덕에 즉시 실행)까지는 되지만 emit으로 깨어난 뒤의 "재개"는
     * 다시 그 디스패처의 큐를 타 버려 `advanceUntilIdle()`로도 실행되지 않는 경우가 있었다.
     * 취소는 여전히 `backgroundScope`의 Job을 부모로 물려받아 테스트 종료 시 자동으로 된다.
     */
    private fun TestScope.collectAlerts(
        notifier: TokenUsageRefreshNotifierImpl,
    ): MutableList<TokenUsageAlert> {
        val received = mutableListOf<TokenUsageAlert>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler), start = CoroutineStart.UNDISPATCHED) {
            notifier.alerts.toList(received)
        }
        return received
    }

    private fun usage(usedTokens: Long, dailyLimit: Long?, exceeded: Boolean) = DailyTokenUsage(
        usedTokens = usedTokens,
        dailyLimit = dailyLimit,
        exceeded = exceeded,
    )
}
