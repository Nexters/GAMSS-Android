package com.gamss.android.feature.calendar.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.designsystem.card.GamssEmotionCard
import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.card.toGamssEmotionCardCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.feature.calendar.R
import java.time.format.DateTimeFormatter

/**
 * 캘린더에서 카드를 탭했을 때 뜨는 상세 팝업.
 *
 * Figma의 Dim + Card 오버레이 구조를 그대로 옮긴다. 배경 dim 은 [Dialog] 창이 기본으로
 * 그려 주므로 여기서 따로 그리지 않는다.
 */
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
                title = card.summary,
                description = card.message,
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

@Composable
private fun CardCloseButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.calendar_card_close_description),
            tint = GamssTheme.colors.gray300,
            modifier = Modifier.size(GamssTheme.spacing.spacing400),
        )
    }
}

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
