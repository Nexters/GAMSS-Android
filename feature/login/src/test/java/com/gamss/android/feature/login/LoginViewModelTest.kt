package com.gamss.android.feature.login

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.LoginResult
import com.gamss.android.domain.auth.LoginUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.test

/**
 * `googleSignInRequestId`를 매개로 [LoginScreen]의 `LaunchedEffect`가 Google Credential 요청을
 * 트리거/재시도하는 구조이므로(구성 변경 시 콜백이 유실되던 문제의 수정 지점), 매 요청마다
 * id가 새로 발급되고 모든 종료 경로(성공/실패/취소)에서 id와 로딩 상태가 확실히 초기화되는지 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val loginUseCase: LoginUseCase = mockk()

    private fun viewModel() = LoginViewModel(loginUseCase)

    @Test
    fun `Google 로그인을 요청하면 로딩 상태와 함께 요청 id가 발급된다`() = runTest {
        viewModel().test(this) {
            containerHost.requestGoogleSignIn()

            expectState { copy(isLoading = true, googleSignInRequestId = 1L) }
        }
    }

    @Test
    fun `이전 요청이 정리되기 전에 다시 요청해도 매번 다른 요청 id가 발급된다`() = runTest {
        // LaunchedEffect(state.googleSignInRequestId)는 id가 바뀔 때만 재실행된다.
        // 이전 요청이 아직 소비되지 않은 상태에서 다시 요청되어도 id가 같은 값으로
        // 겹치면 재요청이 트리거되지 않으므로, 매 요청마다 id가 달라져야 한다.
        viewModel().test(this) {
            containerHost.requestGoogleSignIn()
            val firstState = awaitState()
            assertEquals(1L, firstState.googleSignInRequestId)

            containerHost.requestGoogleSignIn()
            val secondState = awaitState()
            assertEquals(2L, secondState.googleSignInRequestId)
        }
    }

    @Test
    fun `Credential 인증에 성공하면 요청 id를 비우고 로그인 결과에 따라 로딩을 종료한다`() = runTest {
        coEvery { loginUseCase(any()) } returns AppResult.Success(LoginResult(isFirstLogin = false))

        viewModel().test(this) {
            containerHost.requestGoogleSignIn()
            expectState { copy(isLoading = true, googleSignInRequestId = 1L) }

            containerHost.onGoogleCredentialResolved("google-id-token")
            expectState { copy(googleSignInRequestId = null) }
            expectState { copy(isLoading = false) }
            expectSideEffect(LoginSideEffect.LoginSucceeded(isFirstLogin = false))
        }

        coVerify(exactly = 1) { loginUseCase("google-id-token") }
    }

    @Test
    fun `첫 로그인에 성공하면 온보딩 분기에 사용할 결과를 전달한다`() = runTest {
        coEvery { loginUseCase(any()) } returns AppResult.Success(LoginResult(isFirstLogin = true))

        viewModel().test(this) {
            containerHost.onGoogleCredentialResolved("google-id-token")

            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false) }
            expectSideEffect(LoginSideEffect.LoginSucceeded(isFirstLogin = true))
        }
    }

    @Test
    fun `Credential 요청이 실패하면 요청 id와 로딩 상태를 초기화하고 토스트를 노출한다`() = runTest {
        viewModel().test(this) {
            containerHost.requestGoogleSignIn()
            expectState { copy(isLoading = true, googleSignInRequestId = 1L) }

            containerHost.onGoogleSignInFailed()
            expectState { copy(isLoading = false, googleSignInRequestId = null) }
            expectSideEffect(LoginSideEffect.ShowToast("Google 로그인에 실패했어요"))
        }

        coVerify(exactly = 0) { loginUseCase(any()) }
    }

    @Test
    fun `Credential 요청이 취소되면 요청 id와 로딩 상태만 초기화하고 별도 토스트는 없다`() = runTest {
        viewModel().test(this) {
            containerHost.requestGoogleSignIn()
            expectState { copy(isLoading = true, googleSignInRequestId = 1L) }

            containerHost.onGoogleSignInCancelled()
            expectState { copy(isLoading = false, googleSignInRequestId = null) }

            expectNoItems()
        }

        coVerify(exactly = 0) { loginUseCase(any()) }
    }
}
