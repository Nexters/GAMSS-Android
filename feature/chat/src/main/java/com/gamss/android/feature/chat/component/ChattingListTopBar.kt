package com.gamss.android.feature.chat.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            showLeftIcon = isSelectionMode,
            leftIconContentDescription = stringResource(R.string.chatting_list_selection_cancel),
            onLeftIconClick = onSelectionCancel,
            rightActions = listOf(
                GamssTopNavigationIconAction(
                    icon = GamssTopNavigationIcon.Menu,
                    onClick = onMenuClick,
                    contentDescription = stringResource(R.string.chatting_list_menu_content_description),
                ),
            ),
        )

        if (!isSelectionMode) {
            // GamssTopNavigation의 오른쪽에 복수의 아이콘 받는 컴포넌트로 변경되었을때 이 컴포넌트 삭제 예정 (검색 모드 진입을 위한 임시조치)
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-42).dp, y = 3.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(
                        R.string.chatting_list_search_content_description,
                    ),
                    tint = GamssTheme.colors.gray900,
                )
            }
        }
    }
}
