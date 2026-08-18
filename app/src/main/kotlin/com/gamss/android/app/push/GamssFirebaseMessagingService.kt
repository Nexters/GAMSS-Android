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
import kotlinx.coroutines.cancel
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
        val content = message.toNotificationContentOrNull() ?: return
        showNotification(content)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // 권한 확인이 모듈 경계 너머에 있어 lint 가 추적하지 못한다.
    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun showNotification(content: PushNotificationContent) {
        if (!notificationPermissionChecker.isGranted()) return

        try {
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
                .setContentTitle(content.title)
                .setContentText(content.body)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()

            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.w(TAG, "failed to show push notification", e)
        }
    }
}

internal data class PushNotificationContent(
    val title: String,
    val body: String?,
)

internal fun RemoteMessage.toNotificationContentOrNull(): PushNotificationContent? {
    val title = notification?.title ?: data["title"] ?: return null
    val body = notification?.body ?: data["body"]
    return PushNotificationContent(title, body)
}
