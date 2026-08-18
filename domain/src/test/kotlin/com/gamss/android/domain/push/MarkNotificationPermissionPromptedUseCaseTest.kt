package com.gamss.android.domain.push

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkNotificationPermissionPromptedUseCaseTest {

    @Test
    fun `안내 이력을 기록한다`() = runBlocking {
        val promptHistory = FakeNotificationPermissionPromptHistory()
        val useCase = MarkNotificationPermissionPromptedUseCase(promptHistory)

        useCase()

        assertEquals(1, promptHistory.markPromptedCallCount)
        assertTrue(promptHistory.hasPrompted())
    }
}
