package com.gamss.android.feature.home

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.takeWithinMessageLimit
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

internal const val LAST_CHARACTER_BLOCKED = "한 명은 남겨 주세요"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
) : ViewModel(), ContainerHost<HomeState, HomeSideEffect> {

    // init 대신 onCreate 를 쓴다. 구독 시점에 한 번 돌고, 테스트에서 실행 시점을 잡을 수 있다.
    override val container = container<HomeState, HomeSideEffect>(HomeState()) { loadUserInfo() }

    fun navigateToSetting() = intent {
        postSideEffect(HomeSideEffect.NavigateToSetting)
    }

    fun onInputChange(text: String) = blockingIntent {
        reduce { state.copy(input = text.takeWithinMessageLimit()) }
    }

    fun onEmotionPickerToggle() = intent {
        reduce { state.copy(isEmotionPickerExpanded = !state.isEmotionPickerExpanded) }
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

    fun onSubmit() = intent {
        // 인텐트는 동시에 돌 수 있어서 읽고 비우기를 한 reduce 안에서 끝낸다.
        // reduce 는 CAS 재시도로 여러 번 실행되고 마지막 실행만 커밋된다.
        var pending: String? = null
        var excluded: Set<EmotionCharacter> = emptySet()
        reduce {
            pending = state.input.takeIf { it.isNotBlank() }
            excluded = state.excludedCharacters
            if (pending == null) state else state.copy(input = "", isEmotionPickerExpanded = false)
        }
        val message = pending ?: return@intent
        postSideEffect(HomeSideEffect.StartConversation(message, excluded))
    }

    private suspend fun Syntax<HomeState, HomeSideEffect>.loadUserInfo() {
        // 닉네임을 못 받아도 화면은 성립한다. 세션이 끊긴 경우는 AuthRepository 가 로그인으로 되돌린다.
        val nickname = (getUserInfoUseCase() as? AppResult.Success)?.data?.nickname
        reduce { state.copy(isLoading = false, nickname = nickname) }
    }
}
