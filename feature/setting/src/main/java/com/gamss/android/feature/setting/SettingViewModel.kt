package com.gamss.android.feature.setting

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.user.GetUserInfoUseCase
import com.gamss.android.domain.user.SecessionUseCase
import com.gamss.android.domain.user.UpdateNicknameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val secessionUseCase: SecessionUseCase,
) : ViewModel(), ContainerHost<SettingState, SettingSideEffect> {

    override val container = container<SettingState, SettingSideEffect>(SettingState())

    fun loadUserInfo() = intent {
        reduce { state.copy(isLoading = true) }

        when (val result = getUserInfoUseCase()) {
            is AppResult.Success -> reduce {
                state.copy(
                    isLoading = false,
                    userProfile = result.data,
                    nicknameInput = result.data.nickname,
                )
            }

            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(SettingSideEffect.LoadUserInfoFailure)
            }
        }
    }

    fun onNicknameInputChange(nickname: String) = intent {
        reduce { state.copy(nicknameInput = nickname) }
    }

    fun updateNickname() = intent {
        reduce { state.copy(isLoading = true) }
        when (val result = updateNicknameUseCase(state.nicknameInput)) {
            is AppResult.Success -> {
                reduce {
                    state.copy(
                        isLoading = false,
                        userProfile = result.data,
                        nicknameInput = result.data.nickname,
                    )
                }
                postSideEffect(SettingSideEffect.UpdateNicknameSuccess)
            }

            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(SettingSideEffect.UpdateNicknameFailure(result.throwable))
            }
        }
    }

    fun secession() = intent {
        reduce { state.copy(isLoading = true) }
        when (secessionUseCase()) {
            is AppResult.Success -> Unit // 화면 이동은 AuthRepository의 세션 상태 변경을 통해 처리된다.
            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(SettingSideEffect.SecessionFailure)
            }
        }
    }
}
