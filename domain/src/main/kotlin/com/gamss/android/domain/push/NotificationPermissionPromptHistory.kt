package com.gamss.android.domain.push

/** 시스템 권한 팝업에서 한 번 거절했는지 기록한다. OS 가 알려 주지 않는 값이라 직접 남긴다. */
interface NotificationPermissionPromptHistory {

    suspend fun hasPrompted(): Boolean

    suspend fun markPrompted()
}
