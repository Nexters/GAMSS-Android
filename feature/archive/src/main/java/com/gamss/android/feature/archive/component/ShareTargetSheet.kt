package com.gamss.android.feature.archive.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.archive.R

/**
 * 카드를 어디에 공유할지 고르는 시트.
 *
 * 보내는 것이 대상마다 달라 시스템 공유 시트를 쓰지 않는다. 카카오톡은 링크와 한 줄 문구를,
 * 인스타그램은 카드 그림을 보내므로 두 경로를 나눠 두고 고르게 한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareTargetSheet(
    onKakaoTalkClick: () -> Unit,
    onInstagramStoryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GamssTheme.colors.white,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = null,
        // 디자인 여백은 화면 맨 아래까지 재는 값이라, 시트가 인셋을 예약하면 그만큼 더 벌어진다.
        contentWindowInsets = { WindowInsets(0) },
    ) {
        ShareTargetSheetContent(
            onKakaoTalkClick = onKakaoTalkClick,
            onInstagramStoryClick = onInstagramStoryClick,
            onDismiss = onDismiss,
        )
    }
}

/** 시트 창 없이도 모양을 볼 수 있도록 내용만 따로 둔다. [ModalBottomSheet] 는 프리뷰에 그려지지 않는다. */
@Composable
private fun ShareTargetSheetContent(
    onKakaoTalkClick: () -> Unit,
    onInstagramStoryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = sheetBottomPadding())) {
        SheetCloseButton(
            contentDescription = stringResource(R.string.archive_card_share_sheet_close),
            onClick = onDismiss,
        )
        Text(
            text = stringResource(R.string.archive_card_share_sheet_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SheetHorizontalPadding),
            style = GamssTheme.typography.title3,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.height(TitleToTargetsGap))
        ShareTargetRow(
            label = stringResource(R.string.archive_card_share_target_kakaotalk),
            onClick = onKakaoTalkClick,
        )
        ShareTargetRow(
            label = stringResource(R.string.archive_card_share_target_instagram_story),
            onClick = onInstagramStoryClick,
        )
    }
}

@Composable
private fun ColumnScope.ShareTargetRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SheetHorizontalPadding, vertical = TargetVerticalPadding),
        style = GamssTheme.typography.body3Medium,
        color = GamssTheme.colors.gray900,
    )
}

private val TitleToTargetsGap = 8.dp
private val TargetVerticalPadding = 16.dp

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ShareTargetSheetContentPreview() {
    GamssTheme(darkTheme = false) {
        ShareTargetSheetContent(
            onKakaoTalkClick = {},
            onInstagramStoryClick = {},
            onDismiss = {},
        )
    }
}
