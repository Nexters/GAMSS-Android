package com.gamss.android.domain.push

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldPromptNotificationPermissionUseCaseTest {

    @Test
    fun `권한이 없고 거절한 적이 없으면 요청한다`() = runBlocking {
        assertTrue(useCase(granted = false, prompted = false)(false))
    }

    @Test
    fun `한 번 거절해 OS 가 다시 물을 수 있으면 요청한다`() = runBlocking {
        assertTrue(useCase(granted = false, prompted = true)(true))
    }

    @Test
    fun `거절한 뒤 OS 가 더 묻지 않으면 요청하지 않는다`() = runBlocking {
        assertFalse(useCase(granted = false, prompted = true)(false))
    }

    @Test
    fun `권한이 이미 허용되어 있으면 요청하지 않는다`() = runBlocking {
        assertFalse(useCase(granted = true, prompted = false)(false))
    }

    private fun useCase(granted: Boolean, prompted: Boolean) = ShouldPromptNotificationPermissionUseCase(
        notificationPermissionChecker = FakeNotificationPermissionChecker(granted = granted),
        promptHistory = FakeNotificationPermissionPromptHistory(prompted = prompted),
    )
}
