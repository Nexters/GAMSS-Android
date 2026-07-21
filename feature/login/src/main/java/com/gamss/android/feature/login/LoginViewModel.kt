package com.gamss.android.feature.login

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel(), ContainerHost<LoginState, LoginSideEffect> {

    override val container = container<LoginState, LoginSideEffect>(LoginState())

    fun login() = intent {
        reduce { state.copy(isLoading = true) }

        // mock userId. 실제 Google 로그인(Credential Manager) 연동 시 idToken 기반으로 교체한다.
        when (authRepository.login(userId = "mock-user-id")) {
            is AppResult.Success -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(LoginSideEffect.NavigateToMain)
            }

            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(LoginSideEffect.ShowToast("로그인에 실패했어요"))
            }
        }
    }
}
