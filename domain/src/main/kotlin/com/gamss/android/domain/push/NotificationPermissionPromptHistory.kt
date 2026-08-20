package com.gamss.android.domain.push

/**
 * 알림 권한 안내를 이미 노출했는지 기록한다.
 *
 * 시스템은 한 번 거절된 권한에 대해 다이얼로그를 다시 띄우지 않는다. 이력을 남기지 않으면
 * 실행마다 요청을 반복해 사용자의 거절을 무시하는 셈이 되므로 결정 시점을 기억한다.
 */
interface NotificationPermissionPromptHistory {

    suspend fun hasPrompted(): Boolean

    suspend fun markPrompted()
}
