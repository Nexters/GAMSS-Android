package com.gamss.android.feature.chat.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.chat.R

/**
 * 날짜 묶음의 머리 행. 첫 묶음에만 오른쪽에 삭제 액션을 둡니다.
 *
 * 디자인이 날짜와 삭제 액션을 한 줄에 두었기 때문입니다. 화면 상단에 따로 액션 줄을 만들면 첫
 * 날짜와 한 줄로 맞지 않습니다.
 *
 * [dateLabel]이 null이면 왼쪽을 비웁니다. 모든 방이 시각을 모를 때 액션이 놓일 자리가 사라지지
 * 않게 하기 위함입니다.
 */
@Composable
internal fun ChattingListSectionHeader(
    dateLabel: String?,
    showDeleteAction: Boolean,
    deleteActionEnabled: Boolean,
    onDeleteActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (dateLabel != null) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dateLabel != null) {
            Text(
                text = dateLabel,
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray950,
            )
        }

        if (showDeleteAction) {
            Text(
                modifier = Modifier.noRippleClickableIfNotNull(
                    onDeleteActionClick.takeIf { deleteActionEnabled },
                ),
                text = stringResource(R.string.chatting_list_delete_action),
                style = GamssTheme.typography.body5Medium,
                color = if (deleteActionEnabled) {
                    GamssTheme.colors.gray500
                } else {
                    GamssTheme.colors.gray300
                },
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChattingListSectionHeaderLightPreview() {
    GamssTheme(darkTheme = false) {
        ChattingListSectionHeaderPreviewContent()
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
private fun ChattingListSectionHeaderDarkPreview() {
    GamssTheme(darkTheme = true) {
        ChattingListSectionHeaderPreviewContent()
    }
}

@Composable
private fun ChattingListSectionHeaderPreviewContent() {
    Column(
        modifier = Modifier.padding(GamssTheme.spacing.spacing400),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
    ) {
        ChattingListSectionHeader(
            dateLabel = "26.07.31",
            showDeleteAction = true,
            deleteActionEnabled = true,
            onDeleteActionClick = {},
        )
        ChattingListSectionHeader(
            dateLabel = "26.07.30",
            showDeleteAction = true,
            deleteActionEnabled = false,
            onDeleteActionClick = {},
        )
        ChattingListSectionHeader(
            dateLabel = "26.07.29",
            showDeleteAction = false,
            deleteActionEnabled = false,
            onDeleteActionClick = {},
        )
    }
}
