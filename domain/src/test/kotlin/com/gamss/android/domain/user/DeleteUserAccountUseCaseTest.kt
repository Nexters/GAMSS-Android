package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.LoginResult
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.model.DailyTokenUsage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteUserAccountUseCaseTest {

    @Test
    fun `회원 탈퇴 성공 후 로그아웃을 호출한다`() = runBlocking {
        val callOrder = mutableListOf<String>()
        val userRepository = FakeUserRepository(callOrder = callOrder)
        val authRepository = FakeAuthRepository(callOrder = callOrder)
        val useCase = DeleteUserAccountUseCase(userRepository, authRepository)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, authRepository.logoutCallCount)
        assertEquals(listOf("deleteUserAccount", "logout"), callOrder)
    }

    @Test
    fun `회원 탈퇴 실패 시 로그아웃하지 않고 탈퇴 실패를 반환한다`() = runBlocking {
        val failure = IllegalStateException("deleteUserAccount failed")
        val userRepository = FakeUserRepository(
            deleteUserAccountResult = AppResult.Failure(failure),
        )
        val authRepository = FakeAuthRepository()
        val useCase = DeleteUserAccountUseCase(userRepository, authRepository)

        val result = useCase()

        assertSame(failure, (result as AppResult.Failure).throwable)
        assertEquals(0, authRepository.logoutCallCount)
    }

    @Test
    fun `회원 탈퇴 성공 후 로그아웃 실패해도 탈퇴 성공을 반환한다`() = runBlocking {
        val failure = IllegalStateException("logout failed")
        val userRepository = FakeUserRepository()
        val authRepository = FakeAuthRepository(
            logoutResult = AppResult.Failure(failure),
        )
        val useCase = DeleteUserAccountUseCase(userRepository, authRepository)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, authRepository.logoutCallCount)
    }

    @Test(expected = CancellationException::class)
    fun `회원 탈퇴 취소는 실패로 변환하지 않고 전파한다`() = runBlocking {
        val userRepository = FakeUserRepository(
            deleteUserAccountFailure = CancellationException(),
        )
        val authRepository = FakeAuthRepository()

        DeleteUserAccountUseCase(userRepository, authRepository)()
        Unit
    }

    private class FakeUserRepository(
        private val deleteUserAccountResult: AppResult<Unit> = AppResult.Success(Unit),
        private val deleteUserAccountFailure: Throwable? = null,
        private val callOrder: MutableList<String> = mutableListOf(),
    ) : UserRepository {
        override suspend fun updateNickname(nickname: String): AppResult<UserProfile> =
            error("Not needed for this test")

        override suspend fun deleteUserAccount(): AppResult<Unit> {
            callOrder += "deleteUserAccount"
            deleteUserAccountFailure?.let { throw it }
            return deleteUserAccountResult
        }

        override suspend fun getUserInfo(): AppResult<UserProfile> =
            error("Not needed for this test")

        override suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage> =
            error("Not needed for this test")
    }

    private class FakeAuthRepository(
        private val logoutResult: AppResult<Unit> = AppResult.Success(Unit),
        private val callOrder: MutableList<String> = mutableListOf(),
    ) : AuthRepository {
        override val sessionState: StateFlow<SessionState> =
            MutableStateFlow(SessionState.Authenticated)

        var logoutCallCount: Int = 0
            private set

        override suspend fun login(googleIdToken: String): AppResult<LoginResult> =
            error("Not needed for this test")

        override suspend fun reissueTokens(): AppResult<Unit> =
            error("Not needed for this test")

        override suspend fun restoreSession(): AppResult<Unit> =
            error("Not needed for this test")

        override suspend fun logout(): AppResult<Unit> {
            callOrder += "logout"
            logoutCallCount++
            return logoutResult
        }
    }
}
