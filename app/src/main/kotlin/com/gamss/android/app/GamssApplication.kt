package com.gamss.android.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.gamss.android.domain.config.InitializeRemoteConfigUseCase
import com.gamss.android.domain.payment.PendingPurchaseVerifier
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

    @Inject
    lateinit var pendingPurchaseVerifier: PendingPurchaseVerifier

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                Log.w(TAG, "application scope task failed", throwable)
            },
    )

    override fun onCreate() {
        super.onCreate()
        // 최초 onStart와 이후 포그라운드 복귀 시 미처리 구매 복구를 요청한다.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = pendingPurchaseVerifier.requestRecovery()
        })
        createNotificationChannel()
        applicationScope.launch { refreshRiskLexicon() }
        applicationScope.launch { initializeRemoteConfig() }
    }

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
