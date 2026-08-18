package com.gamss.android.feature.calendar.component

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.designsystem.card.GamssEmotionCard
import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.card.cardTitleRes
import com.gamss.android.core.ui.card.toGamssEmotionCardCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.feature.archive.R
import java.time.format.DateTimeFormatter

/** 배경 dim 은 [Dialog] 창이 기본으로 그려 주므로 여기서 따로 그리지 않는다. */
@Composable
internal fun CardDetailDialog(
    card: Card,
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
    onViewConversationClick: () -> Unit,
    onShareClick: () -> Unit,
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
            GamssEmotionCard(
                date = card.date.format(CardDetailDateFormatter),
                character = card.character.toGamssEmotionCardCharacter(),
                title = stringResource(card.character.cardTitleRes()),
                description = card.summary,
                primaryActionLabel = stringResource(R.string.calendar_card_discard),
                secondaryActionLabel = stringResource(R.string.calendar_card_view_conversation),
                shareActionLabel = stringResource(R.string.calendar_card_share),
                onPrimaryActionClick = onDiscardClick,
                onSecondaryActionClick = onViewConversationClick,
                onShareClick = onShareClick,
                topEndAction = { CardCloseButton(onClick = onDismiss) },
            )
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
private fun CardCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = CloseButtonCenteringInset, y = -CloseButtonCenteringInset)
            .size(CloseButtonTouchSize)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.calendar_card_close_description),
            tint = GamssTheme.colors.gray300,
            modifier = Modifier.size(CloseIconSize),
        )
    }
}

/** Figma 우상단 닫기 아이콘 크기. */
private val CloseIconSize = 20.dp
private val CloseButtonTouchSize = 48.dp
private val CloseButtonCenteringInset = (CloseButtonTouchSize - CloseIconSize) / 2

private val CardDetailDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yy.MM.dd")

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun CardDetailDialogLightPreview() {
    GamssTheme(darkTheme = false) {
        CardDetailDialogPreviewContent()
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
private fun CardDetailDialogDarkPreview() {
    GamssTheme(darkTheme = true) {
        CardDetailDialogPreviewContent()
    }
}

@Composable
private fun CardDetailDialogPreviewContent() {
    GamssEmotionCard(
        date = "26.08.03",
        character = GamssEmotionCardCharacter.ANGER,
        title = "오늘 화~나네",
        description = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이",
        primaryActionLabel = stringResource(R.string.calendar_card_discard),
        secondaryActionLabel = stringResource(R.string.calendar_card_view_conversation),
        shareActionLabel = stringResource(R.string.calendar_card_share),
        onPrimaryActionClick = {},
        onSecondaryActionClick = {},
        onShareClick = {},
        topEndAction = { CardCloseButton(onClick = {}) },
    )
}
