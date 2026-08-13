package com.gamss.android.app

import android.app.Application
import android.util.Log
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
        applicationScope.launch { refreshRiskLexicon() }
        applicationScope.launch { initializeRemoteConfig() }
    }
}
