package com.gamss.android.domain.push

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UnregisterCurrentDeviceTokenUseCaseTest {

    @Test
    fun `현재 토큰을 해제한다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val useCase = UnregisterCurrentDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider("token-123"),
            deviceTokenRepository = deviceTokenRepository,
        )

        useCase()

        assertEquals(1, deviceTokenRepository.unregisterCallCount)
        assertEquals("token-123", deviceTokenRepository.lastUnregisteredToken)
    }

    @Test
    fun `토큰이 없으면 해제를 호출하지 않는다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val useCase = UnregisterCurrentDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider(null),
            deviceTokenRepository = deviceTokenRepository,
        )

        useCase()

        assertEquals(0, deviceTokenRepository.unregisterCallCount)
    }

    @Test
    fun `해제가 실패해도 예외를 던지지 않는다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository(
            unregisterResult = AppResult.Failure(IllegalStateException("unregister failed")),
        )
        val useCase = UnregisterCurrentDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider("token-123"),
            deviceTokenRepository = deviceTokenRepository,
        )

        useCase()

        assertEquals(1, deviceTokenRepository.unregisterCallCount)
    }
}
