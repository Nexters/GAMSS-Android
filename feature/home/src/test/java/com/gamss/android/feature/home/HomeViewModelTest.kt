package com.gamss.android.feature.home

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.user.GetUserInfoUseCase
import com.gamss.android.domain.user.UserProfile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val getUserInfoUseCase: GetUserInfoUseCase = mockk()
    private val repository = RecordingConversationRepository()

    private fun viewModel(repository: ConversationRepository = this.repository) =
        HomeViewModel(getUserInfoUseCase, conversationSession(repository))

    @Test
    fun `유저 정보를 받아오면 인사말에 쓸 닉네임이 채워진다`() = runTest {
        givenUserInfo(nickname = "이소연")

        viewModel().test(this) {
            containerHost.loadUserInfo()
            expectState { copy(isLoading = false, nickname = "이소연") }
        }
    }

    @Test
    fun `유저 정보 조회에 실패해도 닉네임 없이 화면을 연다`() = runTest {
        coEvery { getUserInfoUseCase() } returns AppResult.Failure(IllegalStateException("boom"))

        viewModel().test(this) {
            containerHost.loadUserInfo()
            expectState { copy(isLoading = false, nickname = null) }
        }
    }

    // 닉네임 변경 화면에서 돌아오면 HomeScreen이 loadUserInfo를 다시 호출한다. 그 경로를 흉내낸다.
    @Test
    fun `닉네임 변경 후 돌아와 다시 불러오면 바뀐 값으로 갱신된다`() = runTest {
        givenUserInfo(nickname = "이소연")

        viewModel().test(this) {
            containerHost.loadUserInfo()
            expectState { copy(isLoading = false, nickname = "이소연") }

            givenUserInfo(nickname = "소연이")
            containerHost.loadUserInfo()
            expectState { copy(nickname = "소연이") }
        }
    }

    @Test
    fun `다시 불러오다 실패해도 이미 보이던 닉네임은 남는다`() = runTest {
        givenUserInfo(nickname = "이소연")

        viewModel().test(this) {
            containerHost.loadUserInfo()
            expectState { copy(isLoading = false, nickname = "이소연") }

            coEvery { getUserInfoUseCase() } returns AppResult.Failure(IllegalStateException("boom"))
            containerHost.loadUserInfo()
            expectNoItems()
        }
    }

    @Test
    fun `걱정을 적고 보내면 대화를 만들고 그 방을 연다`() = runTest {
        val homeViewModel = viewModel()
        val openConversation = async(start = CoroutineStart.UNDISPATCHED) {
            homeViewModel.openConversationEvents.first()
        }

        homeViewModel.test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }
        assertEquals(NEW_ROOM_ID, openConversation.await())
        assertEquals(WORRY, repository.sentContent)
        assertNull(repository.sentConversationId)
    }

    @Test
    fun `전송에 실패하면 입력을 남기고 안내만 띄운다`() = runTest {
        val failing = RecordingConversationRepository(failing = true)
        viewModel(failing).test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectSideEffect(HomeSideEffect.ShowToast(SEND_FAILED))
        }
    }

    @Test
    fun `체크를 해제한 감정만 제외 목록으로 넘어간다`() = runTest {
        viewModel().test(this) {
            containerHost.onEmotionToggle(EmotionCharacter.SADNESS)
            expectState { copy(selectedCharacters = selectedCharacters - EmotionCharacter.SADNESS) }

            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }
        assertEquals(setOf(EmotionCharacter.SADNESS), repository.sentExcludeCharacters)
    }

    @Test
    fun `해제했던 감정을 다시 누르면 제외 목록에서 빠진다`() = runTest {
        viewModel().test(this) {
            containerHost.onEmotionToggle(EmotionCharacter.ANGER)
            expectState { copy(selectedCharacters = selectedCharacters - EmotionCharacter.ANGER) }

            containerHost.onEmotionToggle(EmotionCharacter.ANGER)
            expectState { copy(selectedCharacters = EmotionCharacter.entries.toSet()) }
        }
    }

    @Test
    fun `마지막 한 명은 해제되지 않고 안내만 띄운다`() = runTest {
        viewModel().test(this) {
            val last = EmotionCharacter.JOY
            EmotionCharacter.entries.filter { it != last }.forEach { character ->
                containerHost.onEmotionToggle(character)
                expectState { copy(selectedCharacters = selectedCharacters - character) }
            }

            containerHost.onEmotionToggle(last)
            expectSideEffect(HomeSideEffect.ShowToast(LAST_CHARACTER_BLOCKED))
            expectNoItems()
        }
    }

    @Test
    fun `감정 목록 닫기는 여러 번 들어와도 다시 열리지 않는다`() = runTest {
        viewModel().test(this) {
            containerHost.onEmotionPickerToggle()
            expectState { copy(isEmotionPickerExpanded = true) }

            // 바깥 탭과 Popup dismiss 가 함께 들어오는 경우다.
            containerHost.onEmotionPickerDismiss()
            expectState { copy(isEmotionPickerExpanded = false) }

            containerHost.onEmotionPickerDismiss()
            expectNoItems()
        }
    }

    @Test
    fun `보내면 감정 목록이 닫힌다`() = runTest {
        viewModel().test(this) {
            containerHost.onEmotionPickerToggle()
            expectState { copy(isEmotionPickerExpanded = true) }

            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true, isEmotionPickerExpanded = false) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }
    }

    @Test
    fun `상한을 넘긴 입력은 140자까지만 남고 한 번 알린다`() = runTest {
        viewModel().test(this) {
            containerHost.onInputChange("가".repeat(MAX_MESSAGE_LENGTH + 20))
            expectState { copy(input = "가".repeat(MAX_MESSAGE_LENGTH)) }
            expectSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))
        }
    }

    @Test
    fun `상한을 넘긴 채로 이어 쳐도 다시 알리지 않는다`() = runTest {
        viewModel().test(this) {
            val filled = "가".repeat(MAX_MESSAGE_LENGTH)
            containerHost.onInputChange(filled + "가")
            expectState { copy(input = filled) }
            expectSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))

            // 상태가 그대로라 expectState 는 없고, 안내도 다시 오지 않는다.
            containerHost.onInputChange(filled + "나")
        }
    }

    @Test
    fun `상한 아래로 줄였다 다시 넘기면 또 알린다`() = runTest {
        viewModel().test(this) {
            val filled = "가".repeat(MAX_MESSAGE_LENGTH)
            containerHost.onInputChange(filled + "가")
            expectState { copy(input = filled) }
            expectSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))

            val shorter = "가".repeat(MAX_MESSAGE_LENGTH - 1)
            containerHost.onInputChange(shorter)
            expectState { copy(input = shorter) }

            containerHost.onInputChange(shorter + "나다")
            expectState { copy(input = shorter + "나") }
            expectSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))
        }
    }

    @Test
    fun `보내서 입력을 비운 뒤 다시 넘기면 또 알린다`() = runTest {
        viewModel().test(this) {
            val filled = "가".repeat(MAX_MESSAGE_LENGTH)
            containerHost.onInputChange(filled + "가")
            expectState { copy(input = filled) }
            expectSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()

            containerHost.onInputChange(filled + "나")
            expectState { copy(input = filled) }
            expectSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))
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
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()

            containerHost.onSubmit()
            expectNoItems()
        }
    }

    @Test
    fun `새 대화에는 앞서 보낸 걱정이 문맥으로 실리지 않는다`() = runTest {
        viewModel().test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }
            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()

            containerHost.onInputChange(SECOND_WORRY)
            expectState { copy(input = SECOND_WORRY) }
            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }
        assertEquals(listOf(null, null), repository.sentContextSummaries)
    }

    @Test
    fun `응답이 늦으면 끝날 때까지 전송 중 상태로 잠긴다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val gated = RecordingConversationRepository(gate = gate)
        viewModel(gated).test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            // 응답을 붙든 동안은 여기서 멈춰 있어야 입력과 전송 버튼이 잠긴 채로 남는다.
            expectNoItems()

            gate.complete(Unit)
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }
        assertEquals(1, gated.sendCount)
    }

    @Test
    fun `대화 생성이 완료되면 이동 이벤트를 발행한다`() = runTest {
        val homeViewModel = viewModel()
        val openConversation = async(start = CoroutineStart.UNDISPATCHED) {
            homeViewModel.openConversationEvents.first()
        }

        homeViewModel.test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }

        assertEquals(NEW_ROOM_ID, openConversation.await())
    }

    @Test
    fun `이동 이벤트를 수집하는 화면이 없으면 대화방 이동이 재생되지 않는다`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val gated = RecordingConversationRepository(gate = gate)
        val homeViewModel = viewModel(gated)

        homeViewModel.test(this) {
            containerHost.onInputChange(WORRY)
            expectState { copy(input = WORRY) }

            containerHost.onSubmit()
            expectState { copy(isSending = true) }
            expectNoItems()

            gate.complete(Unit)
            expectState { copy(isSending = false) }
            expectState { copy(input = "") }
            expectNoItems()
        }

        val replayedEvent = withTimeoutOrNull(1) {
            homeViewModel.openConversationEvents.first()
        }
        assertNull(replayedEvent)
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
        const val SECOND_WORRY = "주말에 약속이 겹쳐서 곤란해요"
        const val BLANK = "   "
    }
}
