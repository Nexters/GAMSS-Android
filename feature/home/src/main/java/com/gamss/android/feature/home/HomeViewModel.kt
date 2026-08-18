package com.gamss.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.takeWithinMessageLimit
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

internal const val LAST_CHARACTER_BLOCKED = "한 명은 남겨 주세요"
internal const val SEND_FAILED = "보내지 못했어요. 잠시 후 다시 시도해 주세요."
internal val MESSAGE_LENGTH_EXCEEDED = "메시지는 ${MAX_MESSAGE_LENGTH}자까지 입력할 수 있어요."

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val session: ConversationSession,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    // init 대신 onCreate 를 쓴다. 구독 시점에 한 번 돌고, 테스트에서 실행 시점을 잡을 수 있다.
    override val container = container<HomeState, HomeSideEffect>(HomeState()) { loadUserInfo() }

    fun navigateToSetting() = intent {
        postSideEffect(HomeSideEffect.NavigateToSetting)
    }

    // 이어 치는 동안 토스트가 쌓이지 않게, 넘긴 상태에서 벗어날 때까지 한 번만 알린다.
    private var lengthWarned = false

    fun onInputChange(text: String) = blockingIntent {
        val limited = text.takeWithinMessageLimit()
        reduce { state.copy(input = limited) }

        if (limited != text) {
            if (!lengthWarned) {
                lengthWarned = true
                postSideEffect(HomeSideEffect.ShowToast(MESSAGE_LENGTH_EXCEEDED))
            }
        } else {
            lengthWarned = false
        }
    }

    fun onEmotionPickerToggle() = intent {
        reduce { state.copy(isEmotionPickerExpanded = !state.isEmotionPickerExpanded) }
    }

    // 닫기는 토글과 나눠 둔다. 바깥 탭과 Popup 의 dismiss 가 겹쳐 들어와도 다시 열리면 안 된다.
    fun onEmotionPickerDismiss() = intent {
        reduce { state.copy(isEmotionPickerExpanded = false) }
    }

    fun onEmotionToggle(character: EmotionCharacter) = intent {
        // 검사와 갱신을 한 reduce 안에서 끝낸다. 연타로 마지막 하나까지 빠지면 서버가 거부한다.
        var blocked = false
        reduce {
            val removing = character in state.selectedCharacters
            blocked = removing && state.selectedCharacters.size == 1
            when {
                blocked -> state
                removing -> state.copy(selectedCharacters = state.selectedCharacters - character)
                else -> state.copy(selectedCharacters = state.selectedCharacters + character)
            }
        }
        if (blocked) postSideEffect(HomeSideEffect.ShowToast(LAST_CHARACTER_BLOCKED))
    }

    /**
     * 대화를 여기서 만들고 id 만 넘긴다. 대화방이 만들게 하면 그 화면이 "새로 만들기"와 "열기"를
     * 겸하게 되고, 문구를 NavKey 에 실어 나르느라 중복 전송 가드가 줄줄이 따라붙는다.
     */
    fun onSubmit() = intent {
        // 검사와 비우기를 한 reduce 안에서 끝낸다. reduce 는 CAS 재시도로 여러 번 실행되고
        // 마지막 실행만 커밋되므로, 보낼 값은 밖으로 빼서 그 커밋본을 읽는다.
        var pending: String? = null
        var excluded: Set<EmotionCharacter> = emptySet()
        reduce {
            pending = state.input.takeIf { it.isNotBlank() && !state.isSending }
            excluded = state.excludedCharacters
            if (pending == null) state else state.copy(isSending = true, isEmotionPickerExpanded = false)
        }
        val message = pending ?: return@intent

        val result = session.send(
            conversationId = null,
            content = message,
            replyToMessageId = null,
            excludeCharacters = excluded,
        )
        reduce { state.copy(isSending = false) }

        when (result) {
            is AppResult.Success -> {
                // 제목 예약과 온디바이스 후처리다. 기다리지 않는다. 제목 반영은 데이터 계층이
                // applicationScope 로 돌려서 이 화면을 벗어나도 끊기지 않는다.
                viewModelScope.launch { session.finishSend() }
                reduce { state.copy(input = "") }
                // 비운 입력은 상한과 무관해졌으므로, 다시 넘기면 새로 알린다.
                lengthWarned = false
                postSideEffect(HomeSideEffect.OpenConversation(result.data.message.conversationId))
            }
            // 입력은 남겨 둔다. 실패한 문구를 다시 치게 하면 안 된다.
            is AppResult.Failure -> postSideEffect(HomeSideEffect.ShowToast(SEND_FAILED))
        }
    }

    private suspend fun Syntax<HomeState, HomeSideEffect>.loadUserInfo() {
        // 닉네임을 못 받아도 화면은 성립한다. 세션이 끊긴 경우는 AuthRepository 가 로그인으로 되돌린다.
        val nickname = (getUserInfoUseCase() as? AppResult.Success)?.data?.nickname
        reduce { state.copy(isLoading = false, nickname = nickname) }
    }
}
