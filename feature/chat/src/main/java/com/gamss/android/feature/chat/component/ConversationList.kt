package com.gamss.android.feature.chat.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gamss.android.feature.chat.ChattingListActions
import com.gamss.android.feature.chat.ChattingListState
import com.gamss.android.feature.chat.ConversationGroup
import com.gamss.android.feature.chat.R

/**
 * 날짜 묶음별 헤더와 카드를 쌓은 목록.
 *
 * 간격을 `spacedBy` 로 일괄 적용하지 않습니다. 디자인의 헤더 위아래 간격(10dp)과 카드 사이
 * 간격(8dp)이 달라, 항목별 여백으로 표현해야 두 값을 각각 지킬 수 있습니다.
 */
@Composable
internal fun ConversationList(
    state: ChattingListState,
    actions: ChattingListActions,
    modifier: Modifier = Modifier,
) {
    val untitled = stringResource(R.string.chatting_list_untitled)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = ListContentPadding,
    ) {
        state.groups.forEachIndexed { index, group ->
            if (group.hasHeader(isFirst = index == 0)) {
                dateHeader(
                    group = group,
                    showDeleteAction = index == 0,
                    deleteActionEnabled = state.isDeleteActionEnabled,
                    onDeleteActionClick = actions.onDeleteActionClick,
                )
            }
            conversationCards(group = group, state = state, actions = actions, untitled = untitled)
        }
    }
}

/** 날짜도 삭제 액션도 없으면 빈 행이 여백만 차지한다. */
private fun ConversationGroup.hasHeader(isFirst: Boolean): Boolean = dateLabel != null || isFirst

private fun LazyListScope.dateHeader(
    group: ConversationGroup,
    showDeleteAction: Boolean,
    deleteActionEnabled: Boolean,
    onDeleteActionClick: () -> Unit,
) {
    item(key = "$HEADER_KEY_PREFIX${group.dateLabel ?: UNDATED_GROUP_KEY}") {
        ChattingListSectionHeader(
            modifier = HeaderModifier,
            dateLabel = group.dateLabel,
            showDeleteAction = showDeleteAction,
            deleteActionEnabled = deleteActionEnabled,
            onDeleteActionClick = onDeleteActionClick,
        )
    }
}

private fun LazyListScope.conversationCards(
    group: ConversationGroup,
    state: ChattingListState,
    actions: ChattingListActions,
    untitled: String,
) {
    // 키를 지정해 삭제 후 재구성 때 선택 상태와 스크롤 위치가 엉키지 않게 한다.
    items(items = group.rows, key = { it.id }) { row ->
        ConversationCard(
            modifier = CardModifier,
            title = row.title ?: untitled,
            timeLabel = row.timeLabel,
            isSelectionMode = state.isSelectionMode,
            isSelected = row.id in state.selectedIds,
            onClick = { actions.onCardClick(row.id) },
            onLongClick = { actions.onCardLongClick(row.id) },
        )
    }
}

private const val HEADER_KEY_PREFIX = "header-"
private const val UNDATED_GROUP_KEY = "undated"

// 좌우 여백이 헤더와 카드에서 다른 것은 디자인 그대로다. 화면 402 기준으로 헤더는 20 + 362,
// 카드는 18 + 366 으로 각각 대칭이다.
private val HeaderModifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
private val CardModifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 8.dp)
private val ListContentPadding = PaddingValues(bottom = 16.dp)
