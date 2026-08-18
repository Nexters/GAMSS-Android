package com.gamss.android.domain.push

import com.gamss.android.core.common.AppResult

internal class FakePushTokenProvider(
    private val token: String? = "token",
) : PushTokenProvider {
    override suspend fun getToken(): String? = token
}

internal class FakeNotificationPermissionChecker(
    private val granted: Boolean = true,
) : NotificationPermissionChecker {
    override fun isGranted(): Boolean = granted
}

internal class FakeDeviceTokenRepository(
    private val registerResult: AppResult<Unit> = AppResult.Success(Unit),
    private val unregisterResult: AppResult<Unit> = AppResult.Success(Unit),
) : DeviceTokenRepository {

    var registerCallCount: Int = 0
        private set
    var unregisterCallCount: Int = 0
        private set
    var lastRegisteredToken: String? = null
        private set
    var lastUnregisteredToken: String? = null
        private set

    override suspend fun registerToken(token: String): AppResult<Unit> {
        registerCallCount++
        lastRegisteredToken = token
        return registerResult
    }

    override suspend fun unregisterToken(token: String): AppResult<Unit> {
        unregisterCallCount++
        lastUnregisteredToken = token
        return unregisterResult
    }
}

internal class FakeNotificationPermissionPromptHistory(
    private var prompted: Boolean = false,
) : NotificationPermissionPromptHistory {

    var markPromptedCallCount: Int = 0
        private set

    override suspend fun hasPrompted(): Boolean = prompted

    override suspend fun markPrompted() {
        markPromptedCallCount++
        prompted = true
    }
}
