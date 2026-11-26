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
    fun `유료 토큰 잔액이 있으면 한도와 잔액을 합친 비율로 계산한다`() = runBlocking {
        // 아직 무료 한도(1200) 안에서 사용 중이라 유료 잔액(300)을 더한 1500이 분모가 된다.
        val repository = FakeUserRepository(
            usage = DailyTokenUsage(usedTokens = 600, dailyLimit = 1200, exceeded = false, paidBalance = 300),
        )

        val result = GetDailyTokenUsageUseCase(repository)()

        assertEquals(40, (result as AppResult.Success).data.usagePercent)
    }

    @Test
    fun `무료 한도를 넘어 유료 토큰을 쓴 뒤에도 하루 총 가용량 기준으로 일관되게 계산한다`() = runBlocking {
        // 무료 한도(1200) + 유료 지갑(300) = 1500 중 1300을 썼으므로 분모는 그대로 1500이어야 한다.
        // 차감된 뒤의 잔액(200)만 그대로 더하면 분모가 1400이 되어 100%를 넘겨버린다.
        val repository = FakeUserRepository(
            usage = DailyTokenUsage(usedTokens = 1300, dailyLimit = 1200, exceeded = false, paidBalance = 200),
        )

        val result = GetDailyTokenUsageUseCase(repository)()

        assertEquals(87, (result as AppResult.Success).data.usagePercent)
    }

    @Test
    fun `유료 지갑까지 완전히 소진되면 사용률은 100퍼센트다`() = runBlocking {
        val repository = FakeUserRepository(
            usage = DailyTokenUsage(usedTokens = 1500, dailyLimit = 1200, exceeded = true, paidBalance = 0),
        )

        val result = GetDailyTokenUsageUseCase(repository)()

        assertEquals(100, (result as AppResult.Success).data.usagePercent)
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
