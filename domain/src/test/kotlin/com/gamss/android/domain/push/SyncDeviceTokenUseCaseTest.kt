package com.gamss.android.domain.push

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDeviceTokenUseCaseTest {

    @Test
    fun `권한이 있으면 토큰을 등록한다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val useCase = SyncDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider("token-123"),
            notificationPermissionChecker = FakeNotificationPermissionChecker(granted = true),
            deviceTokenRepository = deviceTokenRepository,
        )

        useCase()

        assertEquals(1, deviceTokenRepository.registerCallCount)
        assertEquals("token-123", deviceTokenRepository.lastRegisteredToken)
        assertEquals(0, deviceTokenRepository.unregisterCallCount)
    }

    @Test
    fun `권한이 없으면 토큰을 해제한다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val useCase = SyncDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider("token-123"),
            notificationPermissionChecker = FakeNotificationPermissionChecker(granted = false),
            deviceTokenRepository = deviceTokenRepository,
        )

        useCase()

        assertEquals(1, deviceTokenRepository.unregisterCallCount)
        assertEquals("token-123", deviceTokenRepository.lastUnregisteredToken)
        assertEquals(0, deviceTokenRepository.registerCallCount)
    }

    @Test
    fun `토큰을 가져올 수 없으면 아무 것도 하지 않는다`() = runBlocking {
        val deviceTokenRepository = FakeDeviceTokenRepository()
        val useCase = SyncDeviceTokenUseCase(
            pushTokenProvider = FakePushTokenProvider(null),
            notificationPermissionChecker = FakeNotificationPermissionChecker(granted = true),
            deviceTokenRepository = deviceTokenRepository,
        )

        useCase()

        assertEquals(0, deviceTokenRepository.registerCallCount)
        assertEquals(0, deviceTokenRepository.unregisterCallCount)
    }
}
