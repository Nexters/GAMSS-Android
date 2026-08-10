package com.gamss.android.feature.home

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(HomeState())

    fun navigateToSetting() = intent {
        postSideEffect(HomeSideEffect.NavigateToSetting)
    }

    fun logout() = intent {
        when (logoutUseCase()) {
            is AppResult.Success -> Unit // 화면 이동은 AuthRepository의 세션 상태 변경을 통해 처리된다.
            is AppResult.Failure -> postSideEffect(HomeSideEffect.ShowToast("로그아웃에 실패했어요"))
        }
    }
}
