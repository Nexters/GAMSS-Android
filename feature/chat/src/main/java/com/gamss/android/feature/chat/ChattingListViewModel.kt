package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ChattingListViewModel @Inject constructor(
    private val getConversations: GetConversationsUseCase,
    private val clock: Clock,
) : ViewModel(),
    ContainerHost<ChattingListState, ChattingListSideEffect> {

    override val container = container<ChattingListState, ChattingListSideEffect>(ChattingListState())

    fun load() = intent {
        reduce { state.copy(isLoading = true) }
        when (val result = getConversations(LocalDate.now(clock))) {
            is AppResult.Success -> reduce { state.copy(isLoading = false, conversations = result.data) }
            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ChattingListSideEffect.ShowToast(LOAD_FAILED))
            }
        }
    }

    private companion object {
        const val LOAD_FAILED = "대화 목록을 불러오지 못했어요"
    }
}
