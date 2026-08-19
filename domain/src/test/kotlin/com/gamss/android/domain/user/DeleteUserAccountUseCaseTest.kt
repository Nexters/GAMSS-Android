package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.LoginResult
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.ClearCardCacheUseCase
import com.gamss.android.domain.card.FakeCardRepository
import com.gamss.android.domain.model.DailyTokenUsage
import com.gamss.android.domain.push.DeviceTokenRepository
import com.gamss.android.domain.push.FakeDeviceTokenRepository
import com.gamss.android.domain.push.FakePushTokenProvider
import com.gamss.android.domain.push.UnregisterCurrentDeviceTokenUseCase
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
    fun `탈퇴 전에 디바이스 토큰을 먼저 해제하고 탈퇴 후 로그아웃한다`() = runBlocking {
        val callOrder = mutableListOf<String>()
        val userRepository = FakeUserRepository(callOrder = callOrder)
        val authRepository = FakeAuthRepository(callOrder = callOrder)
        val useCase = createUseCase(
            userRepository = userRepository,
            authRepository = authRepository,
            deviceTokenRepository = FakeCallOrderDeviceTokenRepository(callOrder = callOrder),
            cardRepository = FakeCallOrderCardRepository(callOrder = callOrder),
        )

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, authRepository.logoutCallCount)
        assertEquals(listOf("unregisterToken", "deleteUserAccount", "logout", "clearCardCache"), callOrder)
    }

    @Test
    fun `토큰 해제가 실패해도 탈퇴는 진행한다`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val useCase = createUseCase(
            authRepository = authRepository,
            deviceTokenRepository = FakeDeviceTokenRepository(
                unregisterResult = AppResult.Failure(IllegalStateException("unregister failed")),
            ),
        )

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, authRepository.logoutCallCount)
    }

    @Test
    fun `푸시 토큰이 없으면 해제 없이 탈퇴한다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val authRepository = FakeAuthRepository()
        val useCase = createUseCase(
            authRepository = authRepository,
            deviceTokenRepository = deviceTokenRepository,
            pushToken = null,
        )

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(0, deviceTokenRepository.unregisterCallCount)
        assertEquals(1, authRepository.logoutCallCount)
    }

    @Test
    fun `회원 탈퇴 실패 시 로그아웃하지 않고 탈퇴 실패를 반환한다`() = runBlocking {
        val failure = IllegalStateException("deleteUserAccount failed")
        val authRepository = FakeAuthRepository()
        val cardRepository = RecordingCardRepository()
        val useCase = createUseCase(
            userRepository = FakeUserRepository(
                deleteUserAccountResult = AppResult.Failure(failure),
            ),
            authRepository = authRepository,
            cardRepository = cardRepository,
        )

        val result = useCase()

        assertSame(failure, (result as AppResult.Failure).throwable)
        assertEquals(0, authRepository.logoutCallCount)
        // 탈퇴가 실패하면 계정도 캐시도 그대로 둬야 한다.
        assertEquals(0, cardRepository.clearCacheCallCount)
    }

    @Test
    fun `회원 탈퇴 성공 후 로그아웃 실패해도 탈퇴 성공을 반환하고 카드 캐시를 비운다`() = runBlocking {
        val failure = IllegalStateException("logout failed")
        val authRepository = FakeAuthRepository(logoutResult = AppResult.Failure(failure))
        val cardRepository = RecordingCardRepository()
        val useCase = createUseCase(authRepository = authRepository, cardRepository = cardRepository)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, authRepository.logoutCallCount)
        assertEquals(1, cardRepository.clearCacheCallCount)
    }

    @Test(expected = CancellationException::class)
    fun `회원 탈퇴 취소는 실패로 변환하지 않고 전파한다`() = runBlocking {
        val useCase = createUseCase(
            userRepository = FakeUserRepository(
                deleteUserAccountFailure = CancellationException(),
            ),
        )

        useCase()
        Unit
    }

    private fun createUseCase(
        userRepository: UserRepository = FakeUserRepository(),
        authRepository: AuthRepository = FakeAuthRepository(),
        deviceTokenRepository: DeviceTokenRepository = FakeDeviceTokenRepository(),
        cardRepository: CardRepository = NoOpCardRepository(),
        pushToken: String? = "token-123",
    ) = DeleteUserAccountUseCase(
        userRepository = userRepository,
        authRepository = authRepository,
        unregisterCurrentDeviceToken = UnregisterCurrentDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider(pushToken),
            deviceTokenRepository = deviceTokenRepository,
        ),
        clearCardCache = ClearCardCacheUseCase(cardRepository),
    )

    private class FakeCallOrderCardRepository(
        private val callOrder: MutableList<String>,
    ) : FakeCardRepository() {
        override suspend fun clearCache() {
            callOrder += "clearCardCache"
        }
    }

    private class RecordingCardRepository : FakeCardRepository() {
        var clearCacheCallCount: Int = 0
            private set

        override suspend fun clearCache() {
            clearCacheCallCount++
        }
    }

    /** 이 테스트 스위트가 캐시 삭제 자체를 검증하지 않는 케이스에서, 호출 자체는 조용히 받아만 준다. */
    private class NoOpCardRepository : FakeCardRepository() {
        override suspend fun clearCache() = Unit
    }

    private class FakeCallOrderDeviceTokenRepository(
        private val callOrder: MutableList<String>,
    ) : DeviceTokenRepository {
        override suspend fun registerToken(token: String): AppResult<Unit> =
            error("Not needed for this test")

        override suspend fun unregisterToken(token: String): AppResult<Unit> {
            callOrder += "unregisterToken"
            return AppResult.Success(Unit)
        }
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
