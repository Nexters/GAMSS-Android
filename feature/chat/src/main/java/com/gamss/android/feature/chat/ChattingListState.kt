package com.gamss.android.feature.chat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

/**
 * 같은 날짜에 만들어진 대화 묶음.
 *
 * @param dateLabel 예: "26.07.31". null 이면 시각을 모르는 방들의 묶음이라 헤더 텍스트를 감춘다.
 */
@Immutable
data class ConversationGroup(
    val dateLabel: String?,
    val rows: List<ConversationRow>,
)

/**
 * @param title 서버가 아직 제목을 만들지 않은 방은 null. 대체 문구는 화면이 리소스에서 채운다.
 * @param timeLabel 예: "오전 4:20". null 이면 시각 텍스트를 그리지 않는다.
 */
@Immutable
data class ConversationRow(
    val id: Long,
    val title: String?,
    val timeLabel: String?,
)

@Immutable
data class ChattingSearchState(
    val isActive: Boolean = false,
    val keyword: TextFieldValue = TextFieldValue(),
    val hasSearched: Boolean = false,
    val searchGeneration: Long = 0L,
)

@Immutable
data class ChattingListState(
    val groups: List<ConversationGroup> = emptyList(),
    val phase: ChattingListPhase = ChattingListPhase.Loading,
    val search: ChattingSearchState = ChattingSearchState(),
) {
    val isLoading: Boolean get() = phase is ChattingListPhase.Loading

    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()

    /** 체크박스와 하단 삭제 버튼을 그릴지. 확인·삭제 중에도 무엇을 지우는 중인지는 보여야 한다. */
    val isSelectionMode: Boolean
        get() = phase is ChattingListPhase.Selecting ||
            phase is ChattingListPhase.Confirming ||
            phase is ChattingListPhase.Deleting

    val selectedIds: Set<Long>
        get() = when (val current = phase) {
            is ChattingListPhase.Selecting -> current.selectedIds
            is ChattingListPhase.Confirming -> current.targetIds
            is ChattingListPhase.Deleting -> current.targetIds
            else -> emptySet()
        }

    /** 하단 삭제 버튼을 누를 수 있는지. 확인 중과 삭제 중에는 재실행을 막는다. */
    val canDelete: Boolean
        get() = phase is ChattingListPhase.Selecting && phase.selectedIds.isNotEmpty()

    /** 확인 다이얼로그를 띄울지. */
    val isConfirmingDelete: Boolean get() = phase is ChattingListPhase.Confirming

    /**
     * 헤더의 삭제 액션을 누를 수 있는지. 이 액션은 선택 모드 진입 트리거이고, 실제 실행은 하단
     * 버튼이 맡는다. 그래서 선택 모드에 들어간 뒤에는 비활성이다.
     */
    val isDeleteActionEnabled: Boolean get() = phase is ChattingListPhase.Browsing

    /** 뒤로가기로 선택 모드를 나갈 수 있는지. 삭제 중 취소는 지워진 id 목록을 잃으므로 막는다. */
    val canCancelSelection: Boolean get() = phase is ChattingListPhase.Selecting
}
