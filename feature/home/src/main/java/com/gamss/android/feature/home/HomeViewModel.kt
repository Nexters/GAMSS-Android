package com.gamss.android.feature.home

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(HomeState())

    init {
        loadGreeting()
    }

    fun loadGreeting() = intent {
        reduce { state.copy(isLoading = true) }

        // mock 데이터. 실제 UseCase/Repository 연동 시 교체한다.
        delay(500)
        val mockGreeting = "오늘 하루는 어땠나요?"

        reduce { state.copy(isLoading = false, greeting = mockGreeting) }
        postSideEffect(HomeSideEffect.ShowToast("불러오기 완료"))
    }

    fun logout() = intent {
        when (logoutUseCase()) {
            is AppResult.Success -> Unit // 화면 이동은 AuthEventBus를 통해 전역에서 처리된다.
            is AppResult.Failure -> postSideEffect(HomeSideEffect.ShowToast("로그아웃에 실패했어요"))
        }
    }
}
