package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNicknameUseCaseTest {

    @Test
    fun `공백 닉네임은 누락 실패를 반환하고 저장소를 호출하지 않는다`() = runBlocking {
        val repository = FakeUserRepository()

        val result = UpdateNicknameUseCase(repository)("   ")

        assertTrue((result as AppResult.Failure).throwable is NicknameUpdateException.MissingNickname)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `최소 길이보다 짧은 닉네임은 유효하지 않은 닉네임 실패를 반환한다`() = runBlocking {
        val repository = FakeUserRepository()

        val result = UpdateNicknameUseCase(repository)("가")

        assertTrue((result as AppResult.Failure).throwable is NicknameUpdateException.InvalidLength)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `최대 길이보다 긴 닉네임은 유효하지 않은 닉네임 실패를 반환한다`() = runBlocking {
        val repository = FakeUserRepository()
        val nickname = "가".repeat(NicknamePolicy.MAX_LENGTH + 1)

        val result = UpdateNicknameUseCase(repository)(nickname)

        assertTrue((result as AppResult.Failure).throwable is NicknameUpdateException.InvalidLength)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun `유효한 닉네임은 앞뒤 공백을 제거해 저장소에 전달한다`() = runBlocking {
        val profile = userProfile(nickname = "감쓰")
        val repository = FakeUserRepository(
            updateResult = AppResult.Success(profile),
        )

        val result = UpdateNicknameUseCase(repository)("  감쓰  ")

        assertSame(profile, (result as AppResult.Success).data)
        assertEquals("감쓰", repository.lastNickname)
        assertEquals(1, repository.updateCallCount)
    }

    @Test
    fun `저장소 실패를 그대로 반환한다`() = runBlocking {
        val failure = IllegalStateException("update failed")
        val repository = FakeUserRepository(
            updateResult = AppResult.Failure(failure),
        )

        val result = UpdateNicknameUseCase(repository)("감쓰")

        assertSame(failure, (result as AppResult.Failure).throwable)
    }

    @Test(expected = CancellationException::class)
    fun `닉네임 변경 취소는 실패로 변환하지 않고 전파한다`() = runBlocking {
        val repository = FakeUserRepository(
            updateFailure = CancellationException(),
        )

        UpdateNicknameUseCase(repository)("감쓰")
        Unit
    }

    private class FakeUserRepository(
        private val updateResult: AppResult<UserProfile> = AppResult.Success(userProfile()),
        private val updateFailure: Throwable? = null,
    ) : UserRepository {
        var updateCallCount: Int = 0
            private set

        var lastNickname: String? = null
            private set

        override suspend fun updateNickname(nickname: String): AppResult<UserProfile> {
            updateCallCount++
            lastNickname = nickname
            updateFailure?.let { throw it }
            return updateResult
        }

        override suspend fun deleteUserAccount(): AppResult<Unit> =
            error("Not needed for this test")

        override suspend fun getUserInfo(): AppResult<UserProfile> =
            error("Not needed for this test")
    }

    private companion object {
        fun userProfile(nickname: String = "기존닉네임") = UserProfile(
            id = 1L,
            email = "user@gamss.com",
            nickname = nickname,
            status = "ACTIVE",
            createdAt = "2026-08-05T00:00:00Z",
        )
    }
}
