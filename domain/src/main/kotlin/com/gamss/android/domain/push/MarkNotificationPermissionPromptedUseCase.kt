package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class MarkNotificationPermissionPromptedUseCase @Inject constructor(
    private val promptHistory: NotificationPermissionPromptHistory,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() = promptHistory.markPrompted()
}
