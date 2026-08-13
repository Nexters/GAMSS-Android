package com.gamss.android.feature.chat

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationContent
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIcon
import com.gamss.android.feature.chat.component.ChattingListSectionHeader
import com.gamss.android.feature.chat.component.ConversationCard
import com.gamss.android.feature.chat.component.DeleteConversationDialog
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * 진행 중인 대화 목록.
 *
 * 카드를 길게 누르거나 날짜 헤더의 삭제 액션을 누르면 선택 모드로 들어가고, 그때만 하단에 삭제 버튼이
 * 나타납니다. 삭제는 확인 다이얼로그를 거칩니다.
 *
 * 이 화면은 [androidx.compose.material3.Scaffold]를 쓰지 않습니다. 상위 화면이 이미 Scaffold 로
 * 감싸 하단 탭 inset 을 넘겨주므로, 중첩하면 inset 을 두 번 먹습니다.
 */
@Composable
fun ChattingListScreen(
    onChatClick: (conversationId: Long) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChattingListViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChattingListSideEffect.OpenChatRoom -> onChatClick(sideEffect.conversationId)

            is ChattingListSideEffect.ShowLoadFailed ->
                context.showToast(R.string.chatting_list_load_failed)

            is ChattingListSideEffect.ShowDeleteSucceeded ->
                context.showToast(R.string.chatting_list_delete_succeeded)

            is ChattingListSideEffect.ShowDeletePartiallyFailed ->
                context.showToast(
                    R.string.chatting_list_delete_partially_failed,
                    sideEffect.failedCount,
                )

            is ChattingListSideEffect.ShowDeleteFailed ->
                context.showToast(R.string.chatting_list_delete_failed)

            is ChattingListSideEffect.ShowSessionExpired ->
                context.showToast(R.string.chatting_list_session_expired)
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    BackHandler(enabled = state.canCancelSelection) { viewModel.onSelectionCancel() }

    val actions = remember(viewModel) {
        ChattingListActions(
            onCardClick = viewModel::onCardClick,
            onCardLongClick = viewModel::onCardLongClick,
            onDeleteActionClick = viewModel::onDeleteActionClick,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background),
    ) {
        // 선택 모드에서는 로고 대신 좌측 뒤로가기만 남는다. 그 버튼이 선택 모드를 나가는 유일한
        // 화면상 경로다.
        GamssTopNavigation(
            content = if (state.isSelectionMode) {
                GamssTopNavigationContent.None
            } else {
                GamssTopNavigationContent.Logo
            },
            backgroundColor = GamssTheme.colors.background,
            showLeftIcon = state.isSelectionMode,
            showRightIcon = true,
            leftIconContentDescription = stringResource(R.string.chatting_list_selection_cancel),
            rightIconContentDescription = stringResource(R.string.chatting_list_menu_content_description),
            onLeftIconClick = viewModel::onSelectionCancel,
            onRightIconClick = onMenuClick,
            rightIcon = GamssTopNavigationIcon.Menu,
        )

        ChattingListContent(state = state, actions = actions, modifier = Modifier.weight(1f))

        // 선택 모드에서만 나타난다. 헤더의 삭제 액션은 진입 트리거이고 실행은 이 버튼이 맡는다.
        if (state.isSelectionMode) {
            GamssButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(BottomButtonPadding),
                label = stringResource(R.string.chatting_list_delete_action),
                onClick = viewModel::onDeleteRequest,
                variant = GamssButtonVariant.Destructive,
                enabled = state.canDelete,
            )
        }
    }

    if (state.isConfirmingDelete) {
        DeleteConversationDialog(
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss,
        )
    }
}

@Immutable
private data class ChattingListActions(
    val onCardClick: (Long) -> Unit,
    val onCardLongClick: (Long) -> Unit,
    val onDeleteActionClick: () -> Unit,
)

@Composable
private fun ChattingListContent(
    state: ChattingListState,
    actions: ChattingListActions,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        state.isEmpty -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.chatting_list_empty),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray500,
            )
        }

        else -> ConversationList(state = state, actions = actions, modifier = modifier)
    }
}

@Composable
private fun ConversationList(
    state: ChattingListState,
    actions: ChattingListActions,
    modifier: Modifier = Modifier,
) {
    val untitled = stringResource(R.string.chatting_list_untitled)

    // 간격을 spacedBy 로 일괄 적용하지 않는다. 디자인의 헤더 위아래 간격(10dp)과 카드 사이
    // 간격(8dp)이 다르므로, 항목별 아래 여백으로 표현해야 두 값을 각각 지킬 수 있다.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ListBottomPadding),
    ) {
        state.groups.forEachIndexed { index, group ->
            // 날짜도 액션도 없는 헤더는 내보내지 않는다. 그리면 빈 행이 위아래 여백만 차지한다.
            if (group.dateLabel != null || index == 0) {
                // 키를 지정해 삭제 후 재구성 때 체크 상태와 스크롤 위치가 엉키지 않게 한다.
                item(key = "$HEADER_KEY_PREFIX${group.dateLabel ?: UNDATED_GROUP_KEY}") {
                    ChattingListSectionHeader(
                        modifier = Modifier.padding(
                            start = HeaderHorizontalPadding,
                            end = HeaderHorizontalPadding,
                            top = HeaderVerticalSpacing,
                            bottom = HeaderVerticalSpacing,
                        ),
                        dateLabel = group.dateLabel,
                        showDeleteAction = index == 0,
                        deleteActionEnabled = state.isDeleteActionEnabled,
                        onDeleteActionClick = actions.onDeleteActionClick,
                    )
                }
            }

            items(items = group.rows, key = { it.id }) { row ->
                ConversationCard(
                    modifier = Modifier.padding(
                        start = CardHorizontalPadding,
                        end = CardHorizontalPadding,
                        bottom = CardSpacing,
                    ),
                    title = row.title ?: untitled,
                    timeLabel = row.timeLabel,
                    isSelectionMode = state.isSelectionMode,
                    isSelected = row.id in state.selectedIds,
                    onClick = { actions.onCardClick(row.id) },
                    onLongClick = { actions.onCardLongClick(row.id) },
                )
            }
        }
    }
}

private fun Context.showToast(@StringRes resId: Int, vararg formatArgs: Any) {
    Toast.makeText(this, getString(resId, *formatArgs), Toast.LENGTH_SHORT).show()
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChattingListLightPreview() {
    GamssTheme(darkTheme = false) {
        ChattingListPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChattingListDarkPreview() {
    GamssTheme(darkTheme = true) {
        ChattingListPreviewContent()
    }
}

@Composable
private fun ChattingListPreviewContent() {
    val groups = listOf(
        ConversationGroup(
            dateLabel = "26.07.31",
            rows = listOf(
                ConversationRow(
                    id = 1,
                    title = "너무졸려서 지하철에서 걍 눕고싶엇어",
                    timeLabel = "오전 4:20",
                ),
                ConversationRow(
                    id = 2,
                    title = "부장이랑 싸웠는데 밥도 맛없는 거 먹은 날날날날",
                    timeLabel = "오전 2:43",
                ),
            ),
        ),
        ConversationGroup(
            dateLabel = null,
            rows = listOf(ConversationRow(id = 3, title = null, timeLabel = null)),
        ),
    )
    val actions = ChattingListActions(
        onCardClick = {},
        onCardLongClick = {},
        onDeleteActionClick = {},
    )

    Column(modifier = Modifier.fillMaxSize()) {
        PreviewSection(state = ChattingListState(groups, ChattingListPhase.Browsing), actions = actions)
        PreviewSection(
            state = ChattingListState(groups, ChattingListPhase.Selecting(setOf(1L))),
            actions = actions,
        )
    }
}

@Composable
private fun ColumnScope.PreviewSection(
    state: ChattingListState,
    actions: ChattingListActions,
) {
    ChattingListContent(
        state = state,
        actions = actions,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    )
}

private const val HEADER_KEY_PREFIX = "header-"
private const val UNDATED_GROUP_KEY = "undated"

// 좌우 여백이 헤더와 카드에서 다른 것은 디자인 그대로다. 화면 402 기준으로 헤더는 20 + 362,
// 카드는 18 + 366 으로 각각 대칭이다.
private val CardHorizontalPadding = 18.dp
private val HeaderHorizontalPadding = 20.dp
private val HeaderVerticalSpacing = 10.dp
private val CardSpacing = 8.dp
private val ListBottomPadding = 16.dp
private val BottomButtonPadding = 18.dp
