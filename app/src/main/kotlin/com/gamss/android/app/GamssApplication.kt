package com.gamss.android.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.gamss.android.domain.config.InitializeRemoteConfigUseCase
import com.gamss.android.domain.safety.RefreshRiskLexiconUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GamssApplication"

@HiltAndroidApp
class GamssApplication : Application() {

    @Inject
    lateinit var refreshRiskLexicon: RefreshRiskLexiconUseCase

    @Inject
    lateinit var initializeRemoteConfig: InitializeRemoteConfigUseCase

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                Log.w(TAG, "application scope task failed", throwable)
            },
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        applicationScope.launch { refreshRiskLexicon() }
        applicationScope.launch { initializeRemoteConfig() }
    }

    /** API 26+에서는 채널이 없으면 알림이 표시되지 않는다. 알림 발송 전에 미리 만들어 둔다. */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
}
