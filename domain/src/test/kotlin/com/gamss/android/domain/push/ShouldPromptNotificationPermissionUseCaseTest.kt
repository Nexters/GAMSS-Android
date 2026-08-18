package com.gamss.android.domain.push

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldPromptNotificationPermissionUseCaseTest {

    @Test
    fun `권한이 없고 안내 이력도 없으면 요청한다`() = runBlocking {
        val useCase = ShouldPromptNotificationPermissionUseCase(
            notificationPermissionChecker = FakeNotificationPermissionChecker(granted = false),
            promptHistory = FakeNotificationPermissionPromptHistory(prompted = false),
        )

        assertTrue(useCase())
    }

    @Test
    fun `이미 안내한 적이 있으면 요청하지 않는다`() = runBlocking {
        val useCase = ShouldPromptNotificationPermissionUseCase(
            notificationPermissionChecker = FakeNotificationPermissionChecker(granted = false),
            promptHistory = FakeNotificationPermissionPromptHistory(prompted = true),
        )

        assertFalse(useCase())
    }

    @Test
    fun `권한이 이미 허용되어 있으면 요청하지 않는다`() = runBlocking {
        val useCase = ShouldPromptNotificationPermissionUseCase(
            notificationPermissionChecker = FakeNotificationPermissionChecker(granted = true),
            promptHistory = FakeNotificationPermissionPromptHistory(prompted = false),
        )

        assertFalse(useCase())
    }
}
