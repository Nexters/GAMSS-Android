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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gamss.android.feature.chat.component.ChattingListTopBar
import com.gamss.android.feature.chat.component.ConversationList
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

    ChattingListSideEffectHandler(viewModel = viewModel, onChatClick = onChatClick)

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
        ChattingListTopBar(
            isSelectionMode = state.isSelectionMode,
            onSelectionCancel = viewModel::onSelectionCancel,
            onMenuClick = onMenuClick,
        )

        ChattingListBody(state = state, actions = actions, modifier = Modifier.weight(1f))

        if (state.isSelectionMode) {
            DeleteButton(enabled = state.canDelete, onClick = viewModel::onDeleteRequest)
        }
    }

    if (state.isConfirmingDelete) {
        DeleteConversationDialog(
            onConfirm = viewModel::onDeleteConfirm,
            onDismiss = viewModel::onDeleteDismiss,
        )
    }
}

@Composable
private fun ChattingListSideEffectHandler(
    viewModel: ChattingListViewModel,
    onChatClick: (Long) -> Unit,
) {
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
}

@Composable
private fun ChattingListBody(
    state: ChattingListState,
    actions: ChattingListActions,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredBox(modifier) { CircularProgressIndicator() }

        state.isEmpty -> CenteredBox(modifier) {
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
private fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ColumnScope.DeleteButton(enabled: Boolean, onClick: () -> Unit) {
    GamssButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(BottomButtonPadding),
        label = stringResource(R.string.chatting_list_delete_action),
        onClick = onClick,
        variant = GamssButtonVariant.Destructive,
        enabled = enabled,
    )
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
        ChattingListBody(
            state = ChattingListState(groups, ChattingListPhase.Browsing),
            actions = actions,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        ChattingListBody(
            state = ChattingListState(groups, ChattingListPhase.Selecting(setOf(1L))),
            actions = actions,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

private val BottomButtonPadding = 18.dp
