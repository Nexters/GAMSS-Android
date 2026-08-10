package com.gamss.android.feature.home

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.model.SessionState
import com.gamss.android.domain.repository.AuthRepository
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.repository.UserRepository
import com.gamss.android.domain.usecase.GetDailyTokenUsageUseCase
import com.gamss.android.domain.usecase.LogoutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 채팅에서 홈으로 이어지는 갱신 배선을 본다. 표시 형식은 [TokenUsageUiModelTest] 가 본다.
 * orbit 의 test() 는 컨테이너를 초기 상태로 되돌려 init 의 첫 조회를 지우므로 실제 컨테이너를 그대로 쓴다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeTokenUsageRefreshTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 갱신_이벤트가_오면_사용량을_다시_조회한다() = runTest {
        val repository = FakeUserRepository(usedTokens = listOf(12_000, 20_000))
        val notifier = FakeTokenUsageRefreshNotifier()
        val viewModel = viewModel(repository, notifier)

        val loaded = viewModel.container.stateFlow.first { it.tokenUsage != null }
        assertEquals(12_000L, loaded.tokenUsage?.usedTokens)

        notifier.requestRefresh()

        val refreshed = viewModel.container.stateFlow.first { it.tokenUsage?.usedTokens == 20_000L }
        assertEquals(20_000L, refreshed.tokenUsage?.usedTokens)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun 갱신이_실패하면_마지막으로_아는_사용량을_유지한다() = runTest {
        val repository = FakeUserRepository(usedTokens = listOf(12_000), failAfterFirst = true)
        val notifier = FakeTokenUsageRefreshNotifier()
        val viewModel = viewModel(repository, notifier)

        viewModel.container.stateFlow.first { it.tokenUsage != null }

        notifier.requestRefresh()
        repository.awaitCalls(2)

        val afterFailure = viewModel.container.stateFlow.first { !it.isTokenUsageLoading }
        // 실패로 지워버리면 배지가 사라져 사용자가 한도 초과를 알 수 없다.
        assertEquals(12_000L, afterFailure.tokenUsage?.usedTokens)
    }

    private fun viewModel(
        repository: FakeUserRepository,
        notifier: TokenUsageRefreshNotifier,
    ) = HomeViewModel(
        logoutUseCase = LogoutUseCase(FakeAuthRepository),
        getDailyTokenUsageUseCase = GetDailyTokenUsageUseCase(repository),
        tokenUsageRefreshNotifier = notifier,
    )

    private class FakeTokenUsageRefreshNotifier : TokenUsageRefreshNotifier {
        private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        override val refreshEvents: Flow<Unit> = events

        override fun requestRefresh() {
            events.tryEmit(Unit)
        }
    }

    private class FakeUserRepository(
        private val usedTokens: List<Long>,
        private val failAfterFirst: Boolean = false,
    ) : UserRepository {
        private val calls = MutableStateFlow(0)

        val callCount: Int get() = calls.value

        suspend fun awaitCalls(count: Int) {
            calls.first { it >= count }
        }

        override suspend fun updateNickname(nickname: String): AppResult<String> = AppResult.Success(nickname)

        override suspend fun secession(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage> {
            val index = calls.value
            calls.value = index + 1
            if (failAfterFirst && index >= usedTokens.size) {
                return AppResult.Failure(IllegalStateException("token usage unavailable"))
            }
            return AppResult.Success(
                DailyTokenUsage(
                    usedTokens = usedTokens[index.coerceAtMost(usedTokens.lastIndex)],
                    dailyLimit = DAILY_LIMIT,
                    exceeded = false,
                ),
            )
        }
    }

    private object FakeAuthRepository : AuthRepository {
        override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState.Authenticated)

        override suspend fun login(googleIdToken: String): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun reissueTokens(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun restoreSession(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private companion object {
        const val DAILY_LIMIT = 100_000L
    }
}
