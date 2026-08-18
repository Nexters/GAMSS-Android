package com.gamss.android.feature.chat.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationContent
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIcon
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIconAction
import com.gamss.android.feature.chat.R

/**
 * 목록 상단바. 선택 모드에서는 로고 대신 좌측 뒤로가기가 놓이고, 그 버튼이 선택 모드를 나가는
 * 유일한 화면상 경로입니다.
 *
 * 상태 전체가 아니라 [isSelectionMode] 만 받습니다. 선택 항목이 바뀔 때마다 상단바까지 다시
 * 그리지 않기 위함입니다.
 */
@Composable
internal fun ChattingListTopBar(
    isSelectionMode: Boolean,
    onSelectionCancel: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        GamssTopNavigation(
            content = if (isSelectionMode) {
                GamssTopNavigationContent.None
            } else {
                GamssTopNavigationContent.Logo
            },
            backgroundColor = GamssTheme.colors.background,
            showLeftIcon = isSelectionMode,
            onLeftIconClick = onSelectionCancel,
            leftIconContentDescription = stringResource(R.string.chatting_list_selection_cancel),
            rightActions = listOfNotNull(
                // 선택 모드에서는 검색으로 들어갈 수 없다
                if (!isSelectionMode) {
                    GamssTopNavigationIconAction(
                        icon = GamssTopNavigationIcon.Search,
                        onClick = onSearchClick,
                    )
                } else {
                    null
                },
                GamssTopNavigationIconAction(
                    icon = GamssTopNavigationIcon.Menu,
                    onClick = onMenuClick,
                    contentDescription = stringResource(R.string.chatting_list_menu_content_description),
                ),
            ),
        )
    }
}
