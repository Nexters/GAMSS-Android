package com.gamss.android.app.push

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gamss.android.app.MainActivity
import com.gamss.android.app.R
import com.gamss.android.domain.push.NotificationPermissionChecker
import com.gamss.android.domain.push.SyncDeviceTokenUseCase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GamssFcmService"

@AndroidEntryPoint
class GamssFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var syncDeviceToken: SyncDeviceTokenUseCase

    @Inject
    lateinit var notificationPermissionChecker: NotificationPermissionChecker

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                Log.w(TAG, "device token sync failed", throwable)
            },
    )

    override fun onNewToken(token: String) {
        serviceScope.launch { syncDeviceToken() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"]
        showNotification(title, body)
    }

    // notificationPermissionChecker가 POST_NOTIFICATIONS를 이미 확인한다. lint는 모듈 경계를
    // 넘는 이 체크를 추적하지 못해 오탐(MissingPermission)을 낸다.
    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, body: String?) {
        if (!notificationPermissionChecker.isGranted()) return

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
