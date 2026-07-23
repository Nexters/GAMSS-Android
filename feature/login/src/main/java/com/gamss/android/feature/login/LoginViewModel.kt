package com.gamss.android.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

private const val LOGIN_LOG_TAG = "GamssLogin"

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel(), ContainerHost<LoginState, LoginSideEffect> {

    override val container = container<LoginState, LoginSideEffect>(LoginState())

    fun login(googleIdToken: String) = intent {
        reduce { state.copy(isLoading = true) }

        when (loginUseCase(googleIdToken)) {
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

    fun onGoogleSignInFailed() = intent {
        reduce { state.copy(isLoading = false) }
        postSideEffect(LoginSideEffect.ShowToast("Google 로그인에 실패했어요"))
    }
}
