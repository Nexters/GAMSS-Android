package com.gamss.android.app.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamss.android.app.MainActivity
import com.gamss.android.app.R
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamssFirebaseMessagingServiceInstrumentedTest {

    @Test
    fun data_페이로드에서_제목과_본문을_매핑한다() {
        val message = RemoteMessage.Builder("dummy@fcm.googleapis.com")
            .addData("title", "데이터 제목")
            .addData("body", "데이터 본문")
            .build()

        val content = message.toNotificationContentOrNull()

        assertEquals("데이터 제목", content?.title)
        assertEquals("데이터 본문", content?.body)
    }

    @Test
    fun title이_전혀_없으면_null이다() {
        val message = RemoteMessage.Builder("dummy@fcm.googleapis.com").build()

        assertNull(message.toNotificationContentOrNull())
    }

    /**
     * GamssFirebaseMessagingService.showNotification()과 동일한 채널 id·아이콘·인텐트 구성으로
     * 실제 알림을 올려, 매니페스트의 채널/아이콘 리소스 설정이 이 기기의 OS에서 유효한지 확인한다.
     */
    @Test
    fun 서비스와_동일한_구성으로_알림을_올리면_시스템에_표시된다() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.activeNotifications.forEach { notificationManager.cancel(it.id) }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, context.getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("온디바이스_검증_알림")
            .setContentText("instrumented test 로 보낸 메시지")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val id = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(id, notification)

        val posted = waitUntil(timeoutMillis = 3_000) {
            notificationManager.activeNotifications.any { it.id == id }
        }

        assertTrue("알림이 표시되지 않았습니다", posted)
    }

    private fun waitUntil(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(200)
        }
        return condition()
    }
}
