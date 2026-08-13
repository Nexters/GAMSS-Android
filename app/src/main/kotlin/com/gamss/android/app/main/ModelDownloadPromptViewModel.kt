package com.gamss.android.app.main

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.data.model.ModelDownloadConfirmationGateway
import com.gamss.android.domain.model.ObserveOnDeviceModelDownloadStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * emotion/summary 온디바이스 모델 중 하나라도 셀룰러/크기 확인이 필요해지면 app 루트가 배너를
 * 띄울 수 있게 상태를 흘려주고, 사용자가 배너를 눌렀을 때의 확인 요청을 처리한다.
 *
 * Activity 가 필요한 확인 다이얼로그 호출이라 domain 을 거치지 않고 여기서 바로 data 의
 * [ModelDownloadConfirmationGateway] 를 쓴다 — app 은 feature/data 를 모두 볼 수 있는 유일한
 * 모듈이라 이 흐름을 여기 둔다.
 */
@HiltViewModel
class ModelDownloadPromptViewModel @Inject constructor(
    observeStatus: ObserveOnDeviceModelDownloadStatusUseCase,
    private val gateway: ModelDownloadConfirmationGateway,
) : ViewModel() {

    val needsUserConfirmation: StateFlow<Boolean> = observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /** 결과를 기다리지 않는다 — 상태가 바뀌면 위 [needsUserConfirmation] 이 알아서 갱신된다. */
    fun onConfirmDownload(activity: Activity) {
        viewModelScope.launch { gateway.confirmPendingDownloads(activity) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
