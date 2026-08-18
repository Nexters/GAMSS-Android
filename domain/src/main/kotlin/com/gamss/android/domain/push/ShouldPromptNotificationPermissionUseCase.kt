package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class ShouldPromptNotificationPermissionUseCase @Inject constructor(
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val promptHistory: NotificationPermissionPromptHistory,
) : NoParamUseCase<Boolean> {

    override suspend fun invoke(): Boolean =
        !notificationPermissionChecker.isGranted() && !promptHistory.hasPrompted()
}
