package com.gamss.android.feature.setting

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.user.GetUserInfoUseCase
import com.gamss.android.domain.user.DeleteUserAccountUseCase
import com.gamss.android.domain.user.UpdateNicknameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val deleteUserAccountUseCase: DeleteUserAccountUseCase,
) : ViewModel(), ContainerHost<SettingState, SettingSideEffect> {

    override val container = container<SettingState, SettingSideEffect>(SettingState())

    fun loadUserInfo() = intent {
        if (state.isLoading) return@intent

        reduce { state.copy(isLoading = true) }

        when (val result = getUserInfoUseCase()) {
            is AppResult.Success -> reduce {
                state.copy(
                    isLoading = false,
                    userProfile = result.data,
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
        if (state.isLoading) return@intent

        reduce { state.copy(isLoading = true) }
        when (val result = updateNicknameUseCase(state.nicknameInput)) {
            is AppResult.Success -> {
                reduce {
                    state.copy(
                        isLoading = false,
                        userProfile = result.data,
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

    fun deleteUserAccount() = intent {
        if (state.isLoading) return@intent

        reduce { state.copy(isLoading = true) }
        when (deleteUserAccountUseCase()) {
            is AppResult.Success -> {
                reduce { state.copy(isLoading = false) }
                // 화면 이동은 AuthRepository의 세션 상태 변경을 통해 처리된다.
            }

            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(SettingSideEffect.DeleteAccountFailure)
            }
        }
    }
}
