package com.gamss.android.feature.archive.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.GamssTouchTarget
import com.gamss.android.feature.archive.R

/**
 * 카드 한 장을 화면 가운데 띄우는 다이얼로그. 감정 카드와 대화 카드가 같은 자리에서 서로 바뀌므로
 * 위치와 여백을 여기서 한 번만 정한다. 배경 dim 은 [Dialog] 창이 기본으로 그려 준다.
 */
@Composable
internal fun CardDialogScaffold(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GamssTheme.spacing.spacing300),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * 카드가 액션 슬롯을 Figma 값(우상단 28dp)에 맞춰 두므로 아이콘은 슬롯 좌상단에 딱 붙어야 한다.
 * 그런데 터치 영역을 아이콘보다 크게 잡으면 그 차이만큼 아이콘이 안쪽으로 밀리므로,
 * [CloseButtonCenteringInset] 만큼 되돌려 아이콘을 시안 위치로 보낸다.
 *
 * 터치 영역 크기를 [Box] 로 직접 정한다. `IconButton` 은 기본 크기와 최소 터치 크기 적용이
 * 버전마다 달라 되돌릴 양을 코드에서 확정할 수 없다.
 */
@Composable
internal fun CardCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = CloseButtonCenteringInset, y = -CloseButtonCenteringInset)
            .size(GamssTouchTarget.minimum)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.archive_card_close_description),
            tint = GamssTheme.colors.gray300,
            modifier = Modifier.size(CloseIconSize),
        )
    }
}

/** Figma 우상단 닫기 아이콘 크기. */
private val CloseIconSize = 20.dp
private val CloseButtonCenteringInset = (GamssTouchTarget.minimum - CloseIconSize) / 2
