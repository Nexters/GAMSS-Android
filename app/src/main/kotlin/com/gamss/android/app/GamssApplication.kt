package com.gamss.android.app

import android.app.Application
import android.util.Log
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

    /**
     * 백그라운드 초기화 작업의 실패가 앱을 죽이지 않게 한다.
     */
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                Log.w(TAG, "application scope task failed", throwable)
            },
    )

    override fun onCreate() {
        super.onCreate()
        // 위험 표현 사전은 화면 진입 전에 최신화해 둔다. 실패해도 내장 사전으로 동작한다.
        applicationScope.launch { refreshRiskLexicon() }
    }
}
