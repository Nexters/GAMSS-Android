package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.user.UserProfile
import com.gamss.android.domain.user.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class GetDailyTokenUsageUseCaseTest {

    @Test
    fun `저장소 값으로 사용률을 계산해 채운다`() = runBlocking {
        val repository = FakeUserRepository(
            usage = DailyTokenUsage(usedTokens = 60, dailyLimit = 100, exceeded = false),
        )

        val result = GetDailyTokenUsageUseCase(repository)()

        assertEquals(60, (result as AppResult.Success).data.usagePercent)
    }

    @Test
    fun `한도가 없으면 사용률은 null이다`() = runBlocking {
        val repository = FakeUserRepository(
            usage = DailyTokenUsage(usedTokens = 60, dailyLimit = null, exceeded = false),
        )

        val result = GetDailyTokenUsageUseCase(repository)()

        assertNull((result as AppResult.Success).data.usagePercent)
    }

    @Test
    fun `저장소 실패를 그대로 반환한다`() = runBlocking {
        val failure = IllegalStateException("network error")
        val repository = FakeUserRepository(result = AppResult.Failure(failure))

        val result = GetDailyTokenUsageUseCase(repository)()

        assertSame(failure, (result as AppResult.Failure).throwable)
    }

    private class FakeUserRepository(
        usage: DailyTokenUsage = DailyTokenUsage(usedTokens = 0, dailyLimit = 100, exceeded = false),
        private val result: AppResult<DailyTokenUsage> = AppResult.Success(usage),
    ) : UserRepository {
        override suspend fun updateNickname(nickname: String): AppResult<UserProfile> =
            error("Not needed for this test")

        override suspend fun deleteUserAccount(): AppResult<Unit> =
            error("Not needed for this test")

        override suspend fun getUserInfo(): AppResult<UserProfile> =
            error("Not needed for this test")

        override suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage> = result
    }
}
