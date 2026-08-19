package com.gamss.android.feature.archive.component

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.designsystem.card.GamssEmotionCard
import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.card.cardTitleRes
import com.gamss.android.core.ui.card.toGamssEmotionCardCharacter
import com.gamss.android.domain.card.Card
import com.gamss.android.feature.archive.R

@Composable
internal fun CardDetailDialog(
    card: Card,
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
    onViewConversationClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    CardDialogScaffold(onDismiss = onDismiss) {
        GamssEmotionCard(
            date = card.date.format(CardDateFormatter),
            character = card.character.toGamssEmotionCardCharacter(),
            title = stringResource(card.character.cardTitleRes()),
            description = card.summary,
            primaryActionLabel = stringResource(R.string.archive_card_discard),
            secondaryActionLabel = stringResource(R.string.archive_card_view_conversation),
            shareActionLabel = stringResource(R.string.archive_card_share),
            onPrimaryActionClick = onDiscardClick,
            onSecondaryActionClick = onViewConversationClick,
            onShareClick = onShareClick,
            topEndAction = { CardCloseButton(onClick = onDismiss) },
        )
    }
}

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
        primaryActionLabel = stringResource(R.string.archive_card_discard),
        secondaryActionLabel = stringResource(R.string.archive_card_view_conversation),
        shareActionLabel = stringResource(R.string.archive_card_share),
        onPrimaryActionClick = {},
        onSecondaryActionClick = {},
        onShareClick = {},
        topEndAction = { CardCloseButton(onClick = {}) },
    )
}
