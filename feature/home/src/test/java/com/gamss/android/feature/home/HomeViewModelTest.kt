package com.gamss.android.feature.home

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.user.GetUserInfoUseCase
import com.gamss.android.domain.user.UserProfile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val getUserInfoUseCase: GetUserInfoUseCase = mockk()

    private fun viewModel() = HomeViewModel(getUserInfoUseCase)

    @Test
    fun `유저 정보를 받아오면 인사말에 쓸 닉네임이 채워진다`() = runTest {
        givenUserInfo(nickname = "이소연")

        viewModel().test(this) {
            runOnCreate()
            expectState { copy(isLoading = false, nickname = "이소연") }
        }
    }

    @Test
    fun `유저 정보 조회에 실패해도 닉네임 없이 화면을 연다`() = runTest {
        coEvery { getUserInfoUseCase() } returns AppResult.Failure(IllegalStateException("boom"))

        viewModel().test(this) {
            runOnCreate()
            expectState { copy(isLoading = false, nickname = null) }
        }
    }

    @Test
    fun `걱정을 적고 보내면 대화를 시작하고 입력을 비운다`() = runTest {
        viewModel().test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(input = "") }
            expectSideEffect(HomeSideEffect.StartConversation(WORRY))
        }
    }

    @Test
    fun `공백만 적힌 입력은 보내도 대화를 시작하지 않는다`() = runTest {
        viewModel().test(this) {
            containerHost.onInputChange(BLANK)
            expectState { copy(input = BLANK) }

            containerHost.onSubmit()
            expectNoItems()
        }
    }

    @Test
    fun `보낸 뒤 한 번 더 눌러도 빈 입력이라 대화가 두 번 시작되지 않는다`() = runTest {
        viewModel().test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(input = "") }
            expectSideEffect(HomeSideEffect.StartConversation(WORRY))

            containerHost.onSubmit()
            expectNoItems()
        }
    }

    @Test
    fun `설정 아이콘을 누르면 설정으로 이동한다`() = runTest {
        viewModel().test(this) {
            containerHost.navigateToSetting()
            expectSideEffect(HomeSideEffect.NavigateToSetting)
        }
    }

    private fun givenUserInfo(nickname: String?) {
        coEvery { getUserInfoUseCase() } returns AppResult.Success(
            UserProfile(
                id = 1L,
                email = "soyeon@gamss.app",
                nickname = nickname,
                status = "ACTIVE",
                createdAt = null,
            ),
        )
    }

    private companion object {
        const val WORRY = "오늘 발표가 너무 떨려요"
        const val BLANK = "   "
    }
}
