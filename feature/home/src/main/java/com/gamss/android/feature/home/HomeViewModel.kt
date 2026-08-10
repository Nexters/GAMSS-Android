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

    fun refreshDailyTokenUsage() {
        tokenUsageJob?.cancel()
        tokenUsageJob = intent {
            reduce { state.copy(isTokenUsageLoading = true) }

            val result = getDailyTokenUsageUseCase()

            reduce {
                state.copy(
                    isTokenUsageLoading = false,
                    tokenUsage = when (result) {
                        is AppResult.Success -> result.data.toUiModel(state.tokenUsageDisplayMode)
                        is AppResult.Failure -> state.tokenUsage
                    },
                )
            }
        }
    }
}
