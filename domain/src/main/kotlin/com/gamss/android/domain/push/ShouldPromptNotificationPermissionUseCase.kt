package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/**
 * 시스템 권한 팝업을 띄울 수 있을 때만 묻는다. [params] 는 OS 의 shouldShowRequestPermissionRationale 값이다.
 *
 * OS 는 한 번도 묻지 않은 상태와 영구 거절을 똑같이 false 로 보고하므로 거절 이력으로 둘을 구분한다.
 */
class ShouldPromptNotificationPermissionUseCase @Inject constructor(
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val promptHistory: NotificationPermissionPromptHistory,
) : UseCase<Boolean, Boolean> {

    override suspend fun invoke(params: Boolean): Boolean =
        !notificationPermissionChecker.isGranted() && (params || !promptHistory.hasPrompted())
}
