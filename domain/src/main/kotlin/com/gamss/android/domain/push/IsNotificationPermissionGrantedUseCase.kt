package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class IsNotificationPermissionGrantedUseCase @Inject constructor(
    private val notificationPermissionChecker: NotificationPermissionChecker,
) : NoParamUseCase<Boolean> {

    override suspend fun invoke(): Boolean = notificationPermissionChecker.isGranted()
}
