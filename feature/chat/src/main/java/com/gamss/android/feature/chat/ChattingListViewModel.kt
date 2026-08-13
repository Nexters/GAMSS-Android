package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.conversation.DeleteConversationsResult
import com.gamss.android.domain.conversation.DeleteConversationsUseCase
import com.gamss.android.domain.conversation.GetOngoingConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

private typealias ChattingListSyntax = Syntax<ChattingListState, ChattingListSideEffect>

@HiltViewModel
class ChattingListViewModel @Inject constructor(
    private val getOngoingConversations: GetOngoingConversationsUseCase,
    private val deleteConversations: DeleteConversationsUseCase,
) : ViewModel(),
    ContainerHost<ChattingListState, ChattingListSideEffect> {

    override val container = container<ChattingListState, ChattingListSideEffect>(ChattingListState())

    fun load() = intent {
        // 화면 재구성(회전 등)이나 재진입으로 다시 불린다. 목록을 보는 중이 아니면 단계를 건드리지
        // 않는다. 선택이 지워지거나 확인 다이얼로그가 닫히거나 삭제 결과 처리와 뒤집힌다.
        if (state.phase is ChattingListPhase.Selecting ||
            state.phase is ChattingListPhase.Confirming ||
            state.phase is ChattingListPhase.Deleting
        ) {
            return@intent
        }

        // 이미 채워진 목록으로 재진입할 때 스피너를 다시 띄우지 않는다. 화면이 번쩍인다.
        if (state.groups.isEmpty()) {
            reduce { state.copy(phase = ChattingListPhase.Loading) }
        }
        when (val result = getOngoingConversations()) {
            is AppResult.Success -> reduce {
                state.copy(
                    groups = result.data.toConversationGroups(),
                    phase = ChattingListPhase.Browsing,
                )
            }

            is AppResult.Failure -> {
                reduce { state.copy(phase = ChattingListPhase.Browsing) }
                postSideEffect(ChattingListSideEffect.ShowLoadFailed)
            }
        }
    }

    fun onCardClick(conversationId: Long) = intent {
        when (val current = state.phase) {
            is ChattingListPhase.Browsing ->
                postSideEffect(ChattingListSideEffect.OpenChatRoom(conversationId))

            is ChattingListPhase.Selecting -> reduce {
                state.copy(phase = ChattingListPhase.Selecting(current.selectedIds.toggle(conversationId)))
            }

            is ChattingListPhase.Loading,
            is ChattingListPhase.Confirming,
            is ChattingListPhase.Deleting,
            -> Unit
        }
    }

    fun onCardLongClick(conversationId: Long) = intent {
        when (val current = state.phase) {
            is ChattingListPhase.Browsing -> reduce {
                state.copy(phase = ChattingListPhase.Selecting(setOf(conversationId)))
            }

            is ChattingListPhase.Selecting -> reduce {
                state.copy(phase = ChattingListPhase.Selecting(current.selectedIds + conversationId))
            }

            is ChattingListPhase.Loading,
            is ChattingListPhase.Confirming,
            is ChattingListPhase.Deleting,
            -> Unit
        }
    }

    /** 헤더의 삭제 액션. 선택 모드로 들어가기만 한다. 실행은 [onDeleteRequest] 가 맡는다. */
    fun onDeleteActionClick() = intent {
        if (state.phase !is ChattingListPhase.Browsing) return@intent
        reduce { state.copy(phase = ChattingListPhase.Selecting(emptySet())) }
    }

    /** 하단 삭제 버튼. 바로 지우지 않고 확인 단계를 거친다. */
    fun onDeleteRequest() = intent {
        val current = state.phase
        if (current !is ChattingListPhase.Selecting || current.selectedIds.isEmpty()) return@intent
        reduce { state.copy(phase = ChattingListPhase.Confirming(current.selectedIds)) }
    }

    fun onDeleteConfirm() = intent {
        val current = state.phase
        if (current !is ChattingListPhase.Confirming) return@intent
        reduce { state.copy(phase = ChattingListPhase.Deleting(current.targetIds)) }
        runDelete(current.targetIds)
    }

    /** 확인을 물렀을 때. 고른 방은 그대로 두어 바로 다시 실행할 수 있게 한다. */
    fun onDeleteDismiss() = intent {
        val current = state.phase
        if (current !is ChattingListPhase.Confirming) return@intent
        reduce { state.copy(phase = ChattingListPhase.Selecting(current.targetIds)) }
    }

    fun onSelectionCancel() = intent {
        if (state.phase !is ChattingListPhase.Selecting) return@intent
        reduce { state.copy(phase = ChattingListPhase.Browsing) }
    }

    private suspend fun ChattingListSyntax.runDelete(targetIds: Set<Long>) {
        when (val result = deleteConversations(targetIds.toList())) {
            is AppResult.Success -> onDeleteSettled(result.data)
            is AppResult.Failure -> onDeleteFailed(targetIds, result.throwable)
        }
    }

    private suspend fun ChattingListSyntax.onDeleteSettled(result: DeleteConversationsResult) {
        // 일부만 실패해도 선택 모드를 나간다. 사라진 행 때문에 선택 id 가 화면과 어긋난 상태로
        // 남으면, 재조회된 목록의 낡은 체크로 엉뚱한 방을 지울 수 있다.
        reduce { state.copy(phase = ChattingListPhase.Browsing) }

        if (result.failures.isEmpty()) {
            postSideEffect(ChattingListSideEffect.ShowDeleteSucceeded)
        } else {
            postSideEffect(ChattingListSideEffect.ShowDeletePartiallyFailed(result.failures.size))
        }

        reload(knownDeletedIds = result.deletedIds)
    }

    private suspend fun ChattingListSyntax.onDeleteFailed(targetIds: Set<Long>, throwable: Throwable) {
        if (throwable is SessionExpiredException) {
            reduce { state.copy(phase = ChattingListPhase.Browsing) }
            postSideEffect(ChattingListSideEffect.ShowSessionExpired)
            reload(knownDeletedIds = emptyList())
            return
        }

        // 하나도 지우지 못했으니 목록은 그대로다. 선택을 유지해 바로 재시도할 수 있게 한다.
        reduce { state.copy(phase = ChattingListPhase.Selecting(targetIds)) }
        postSideEffect(ChattingListSideEffect.ShowDeleteFailed)
    }

    /**
     * 삭제 뒤 목록 갱신. 로컬 캐시가 없어 서버가 유일한 진실이다.
     *
     * 조회 실패는 알리지 않는다. 삭제 결과를 이미 알린 뒤라 토스트가 겹치면 "삭제했어요" 와
     * "불러오지 못했어요" 가 서로 모순되게 읽힌다. 대신 아는 만큼 로컬에서 지운다.
     * 지운 행이 남아 있으면 사용자가 같은 방을 다시 지우려 한다.
     */
    private suspend fun ChattingListSyntax.reload(knownDeletedIds: List<Long>) {
        when (val result = getOngoingConversations()) {
            is AppResult.Success -> reduce { state.copy(groups = result.data.toConversationGroups()) }
            is AppResult.Failure ->
                reduce { state.copy(groups = state.groups.withoutIds(knownDeletedIds.toSet())) }
        }
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun List<ConversationGroup>.withoutIds(removedIds: Set<Long>): List<ConversationGroup> {
    if (removedIds.isEmpty()) return this
    return mapNotNull { group ->
        val rows = group.rows.filterNot { it.id in removedIds }
        if (rows.isEmpty()) null else group.copy(rows = rows)
    }
}
