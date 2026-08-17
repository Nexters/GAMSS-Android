package com.gamss.android.domain.push

interface NotificationPermissionChecker {

    fun isGranted(): Boolean
}
