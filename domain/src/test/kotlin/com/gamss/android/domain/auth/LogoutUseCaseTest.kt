package com.gamss.android.domain.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.ClearCardCacheUseCase
import com.gamss.android.domain.card.FakeCardRepository
import com.gamss.android.domain.push.FakeDeviceTokenRepository
import com.gamss.android.domain.push.FakePushTokenProvider
import com.gamss.android.domain.push.UnregisterCurrentDeviceTokenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoutUseCaseTest {

    @Test
    fun `로그아웃 전에 디바이스 토큰을 먼저 해제하고 마지막에 카드 캐시를 비운다`() = runBlocking {
        val callOrder = mutableListOf<String>()
        val authRepository = FakeAuthRepository(callOrder = callOrder)
        val deviceTokenRepository = FakeCallOrderDeviceTokenRepository(callOrder = callOrder)
        val useCase = LogoutUseCase(
            authRepository = authRepository,
            unregisterCurrentDeviceToken = UnregisterCurrentDeviceTokenUseCase(
                pushTokenProvider = FakePushTokenProvider("token-123"),
                deviceTokenRepository = deviceTokenRepository,
            ),
            clearCardCache = ClearCardCacheUseCase(FakeCallOrderCardRepository(callOrder)),
        )

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("unregisterToken", "logout", "clearCardCache"), callOrder)
    }

    @Test
    fun `토큰 해제가 실패해도 로그아웃은 진행한다`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val deviceTokenRepository = FakeDeviceTokenRepository(
            unregisterResult = AppResult.Failure(IllegalStateException("unregister failed")),
        )
        val useCase = LogoutUseCase(
            authRepository = authRepository,
            unregisterCurrentDeviceToken = UnregisterCurrentDeviceTokenUseCase(
                pushTokenProvider = FakePushTokenProvider("token-123"),
                deviceTokenRepository = deviceTokenRepository,
            ),
            clearCardCache = ClearCardCacheUseCase(NoOpCardRepository()),
        )

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, authRepository.logoutCallCount)
    }

    @Test
    fun `토큰이 없으면 해제 없이 로그아웃한다`() = runBlocking {
        val authRepository = FakeAuthRepository()
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val useCase = LogoutUseCase(
            authRepository = authRepository,
            unregisterCurrentDeviceToken = UnregisterCurrentDeviceTokenUseCase(
                pushTokenProvider = FakePushTokenProvider(null),
                deviceTokenRepository = deviceTokenRepository,
            ),
            clearCardCache = ClearCardCacheUseCase(NoOpCardRepository()),
        )

        useCase()

        assertEquals(0, deviceTokenRepository.unregisterCallCount)
        assertEquals(1, authRepository.logoutCallCount)
    }

    /** 로그아웃 API 가 실패해도 세션은 로컬에서 Unauthenticated 로 내려가므로 카드 캐시도 비워야 한다. */
    @Test
    fun `로그아웃이 실패해도 카드 캐시는 비운다`() = runBlocking {
        val callOrder = mutableListOf<String>()
        val authRepository = FakeAuthRepository(
            logoutResult = AppResult.Failure(IllegalStateException("logout failed")),
            callOrder = callOrder,
        )
        val useCase = LogoutUseCase(
            authRepository = authRepository,
            unregisterCurrentDeviceToken = UnregisterCurrentDeviceTokenUseCase(
                pushTokenProvider = FakePushTokenProvider(null),
                deviceTokenRepository = FakeDeviceTokenRepository(),
            ),
            clearCardCache = ClearCardCacheUseCase(FakeCallOrderCardRepository(callOrder)),
        )

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        assertEquals(listOf("logout", "clearCardCache"), callOrder)
    }

    private class FakeCallOrderCardRepository(
        private val callOrder: MutableList<String>,
    ) : FakeCardRepository() {
        override suspend fun clearCache() {
            callOrder += "clearCardCache"
        }
    }

    /** 이 테스트 스위트는 토큰 해제 순서만 검증하므로, 캐시 삭제 호출 자체는 조용히 받아만 준다. */
    private class NoOpCardRepository : FakeCardRepository() {
        override suspend fun clearCache() = Unit
    }

    private class FakeCallOrderDeviceTokenRepository(
        private val callOrder: MutableList<String>,
    ) : com.gamss.android.domain.push.DeviceTokenRepository {
        override suspend fun registerToken(token: String): AppResult<Unit> =
            error("Not needed for this test")

        override suspend fun unregisterToken(token: String): AppResult<Unit> {
            callOrder += "unregisterToken"
            return AppResult.Success(Unit)
        }
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
