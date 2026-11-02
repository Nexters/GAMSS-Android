package com.gamss.android.feature.setting.main

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.BuildInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val buildInfo: BuildInfo,
) : ViewModel() {

    /** 디버그 전용 메뉴(현재는 billing 테스트 화면) 노출 여부. 릴리즈 빌드는 항상 false. */
    val isDebugBuild: Boolean get() = buildInfo.isDebug
}
