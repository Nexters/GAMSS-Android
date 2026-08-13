package com.gamss.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.usecase.GetDailyTokenUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyTokenUsageUseCase: GetDailyTokenUsageUseCase,
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    override val container = container<HomeState, HomeSideEffect>(HomeState())

    private var tokenUsageJob: Job? = null

    init {
        refreshDailyTokenUsage()
        viewModelScope.launch {
            tokenUsageRefreshNotifier.refreshEvents.collect { refreshDailyTokenUsage() }
        }
    }

    fun navigateToSetting() = intent {
        postSideEffect(HomeSideEffect.NavigateToSetting)
    }

    private fun refreshDailyTokenUsage() {
        tokenUsageJob?.cancel()
        tokenUsageJob = intent {
            reduce { state.copy(isTokenUsageLoading = true) }

            val usage = when (val result = getDailyTokenUsageUseCase()) {
                is AppResult.Success -> result.data.toUiModel()
                is AppResult.Failure -> null
            }

            reduce {
                state.copy(
                    isTokenUsageLoading = false,
                    tokenUsage = usage ?: state.tokenUsage,
                )
            }
        }
    }
}
