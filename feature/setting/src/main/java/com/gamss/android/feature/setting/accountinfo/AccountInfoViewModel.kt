package com.gamss.android.feature.setting.accountinfo

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.LogoutUseCase
import com.gamss.android.domain.user.DeleteUserAccountUseCase
import com.gamss.android.domain.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class AccountInfoViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val deleteUserAccountUseCase: DeleteUserAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel(), ContainerHost<AccountInfoState, AccountInfoSideEffect> {

    override val container = container<AccountInfoState, AccountInfoSideEffect>(AccountInfoState())

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
                postSideEffect(AccountInfoSideEffect.LoadUserInfoFailure)
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
                postSideEffect(AccountInfoSideEffect.DeleteAccountFailure)
            }
        }
    }

    fun logout() = intent {
        when (logoutUseCase()) {
            is AppResult.Success -> Unit // 화면 이동은 AuthRepository의 세션 상태 변경을 통해 처리된다.
            is AppResult.Failure -> postSideEffect(AccountInfoSideEffect.LogoutFailure)
        }
    }
}
