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
                postSideEffect(SettingSideEffect.ShowToast("사용자 정보를 불러오지 못했어요"))
            }
        }
    }

    fun onNicknameInputChange(nickname: String) = intent {
        reduce { state.copy(nicknameInput = nickname) }
    }

    fun updateNickname() = intent {
        when (val result = updateNicknameUseCase(state.nicknameInput)) {
            is AppResult.Success -> {
                reduce {
                    state.copy(userProfile = result.data, nicknameInput = result.data.nickname)
                }
                postSideEffect(SettingSideEffect.ShowToast("닉네임이 변경되었어요"))
            }

            is AppResult.Failure -> postSideEffect(SettingSideEffect.ShowToast("닉네임 변경에 실패했어요"))
        }
    }

    fun secession() = intent {
        when (secessionUseCase()) {
            is AppResult.Success -> Unit // 화면 이동은 AuthRepository의 세션 상태 변경을 통해 처리된다.
            is AppResult.Failure -> postSideEffect(SettingSideEffect.ShowToast("회원 탈퇴에 실패했어요"))
        }
    }
}
