package com.gamss.android.feature.login

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.auth.LoginUseCase
import com.gamss.android.domain.auth.SessionExpiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel(), ContainerHost<LoginState, LoginSideEffect> {

    override val container = container<LoginState, LoginSideEffect>(LoginState())

    private fun login(googleIdToken: String) = intent {
        reduce { state.copy(isLoading = true) }

        when (val result = loginUseCase(googleIdToken)) {
            is AppResult.Success -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(LoginSideEffect.LoginSucceeded(result.data.isFirstLogin))
            }

            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(LoginSideEffect.ShowToast(result.throwable.toLoginFailureMessage()))
            }
        }
    }

    fun requestGoogleSignIn() = intent {
        reduce {
            state.copy(
                isLoading = true,
                googleSignInRequestId = (state.googleSignInRequestId ?: 0L) + 1L,
            )
        }
    }

    fun onGoogleCredentialResolved(googleIdToken: String) = intent {
        reduce { state.copy(googleSignInRequestId = null) }
        login(googleIdToken)
    }

    fun onGoogleSignInFailed() = intent {
        reduce {
            state.copy(
                isLoading = false,
                googleSignInRequestId = null,
            )
        }
        postSideEffect(LoginSideEffect.ShowToast("Google 로그인에 실패했어요"))
    }

    fun onGoogleSignInCancelled() = intent {
        reduce {
            state.copy(
                isLoading = false,
                googleSignInRequestId = null,
            )
        }
    }

    private fun Throwable.toLoginFailureMessage(): String = when (this) {
        is SessionExpiredException -> "세션이 만료되었어요. 다시 로그인해 주세요"
        is ApiException.Network -> "네트워크 연결을 확인해 주세요"
        is ApiException.Http -> message ?: "로그인에 실패했어요"
        else -> "로그인에 실패했어요"
    }
}
