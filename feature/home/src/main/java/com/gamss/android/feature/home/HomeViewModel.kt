package com.gamss.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.usecase.GetDailyTokenUsageUseCase
import com.gamss.android.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val getDailyTokenUsageUseCase: GetDailyTokenUsageUseCase,
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(HomeState())

    /**
     * 진행 중인 사용량 조회. 갱신 요청이 몰리면 취소해야 하므로 핸들을 들고 있는다.
     * 네트워크 응답 스레드에서 쓰고 이벤트 루프 스레드에서 읽으므로 @Volatile 이 필요하다.
     */
    @Volatile
    private var tokenUsageJob: Job? = null

    init {
        loadGreeting()
        refreshDailyTokenUsage()
        viewModelScope.launch {
            tokenUsageRefreshNotifier.refreshEvents.collect { refreshDailyTokenUsage() }
        }
    }

    fun loadGreeting() = intent {
        reduce { state.copy(isLoading = true) }

        delay(500)
        val mockGreeting = "오늘 하루는 어땠나요?"

        reduce { state.copy(isLoading = false, greeting = mockGreeting) }
        postSideEffect(HomeSideEffect.ShowToast("불러오기 완료"))
    }

    fun logout() = intent {
        when (logoutUseCase()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> postSideEffect(HomeSideEffect.ShowToast("로그아웃에 실패했어요"))
        }
    }

    /**
     * intent 는 즉시 반환해 collect 쪽 취소가 조회까지 닿지 않는다.
     * 이전 조회를 직접 취소해야 응답이 역순으로 도착해도 오래된 사용량이 남지 않는다.
     */
    fun refreshDailyTokenUsage() {
        tokenUsageJob?.cancel()
        tokenUsageJob = intent {
            reduce { state.copy(isTokenUsageLoading = true) }

            val result = getDailyTokenUsageUseCase()

            reduce {
                state.copy(
                    isTokenUsageLoading = false,
                    // 갱신 실패는 마지막으로 아는 사용량을 지우지 않는다.
                    tokenUsage = when (result) {
                        is AppResult.Success -> result.data.toUiModel(state.tokenUsageDisplayMode)
                        is AppResult.Failure -> state.tokenUsage
                    },
                )
            }
        }
    }
}
