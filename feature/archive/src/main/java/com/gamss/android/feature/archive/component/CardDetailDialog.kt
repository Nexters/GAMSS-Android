package com.gamss.android.feature.archive.component

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.common.util.formatCardDate
import com.gamss.android.core.designsystem.card.GamssEmotionCard
import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.core.designsystem.card.GamssEmotionCardFooter
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.card.cardTitleRes
import com.gamss.android.core.ui.card.toGamssEmotionCardCharacter
import com.gamss.android.core.ui.share.CardShareLink
import com.gamss.android.core.ui.share.ComposeCapture
import com.gamss.android.core.ui.share.StoryShareResult
import com.gamss.android.core.ui.share.captureTo
import com.gamss.android.core.ui.share.rememberComposeCapture
import com.gamss.android.core.ui.share.shareBitmapToInstagramStory
import com.gamss.android.core.ui.share.shareTextToKakaoTalk
import com.gamss.android.domain.card.Card
import com.gamss.android.feature.archive.R
import kotlinx.coroutines.launch

@Composable
internal fun CardDetailDialog(
    card: Card,
    isShareSheetVisible: Boolean,
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
    onViewConversationClick: () -> Unit,
    onShareClick: () -> Unit,
    onShareSheetDismiss: () -> Unit,
    onInstagramShareFailed: (StoryShareResult) -> Unit,
    onKakaoTalkShareFailed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val capture = rememberComposeCapture()
    val kakaoTalkText = stringResource(
        R.string.archive_card_share_kakaotalk_text,
        stringResource(card.character.cardTitleRes()),
        CardShareLink.of(card.id),
    )

    // 공유 이미지에는 누를 수 없는 버튼과 닫기 아이콘을 남기지 않는다. 감추는 동안만 켜 두고,
    // 그 상태가 실제로 그려진 뒤에 캡처한다.
    var isCapturing by remember { mutableStateOf(false) }
    // 인스타그램 공유는 캡처가 끝날 때까지 비동기로 이어지므로, 그 사이 재진입해 캡처를 중복
    // 시작하지 않도록 막는다.
    var isSharing by remember { mutableStateOf(false) }

    CardDialogScaffold(onDismiss = onDismiss) {
        GamssEmotionCard(
            date = formatCardDate(card.date),
            character = card.character.toGamssEmotionCardCharacter(),
            title = stringResource(card.character.cardTitleRes()),
            description = card.summary,
            // 캡처하는 동안에는 액션 대신 워드마크를 넣는다. 누를 수 없는 버튼이 그림에
            // 남지 않고, 시안(Figma 4243:18230)의 공유용 카드와 같아진다.
            footer = if (isCapturing) {
                GamssEmotionCardFooter.Brand
            } else {
                GamssEmotionCardFooter.Actions(
                    primaryLabel = stringResource(R.string.archive_card_discard),
                    secondaryLabel = stringResource(R.string.archive_card_view_conversation),
                    shareLabel = stringResource(R.string.archive_card_share),
                    onPrimaryClick = onDiscardClick,
                    onSecondaryClick = onViewConversationClick,
                    onShareClick = onShareClick,
                )
            },
            modifier = Modifier.captureTo(capture, isCapturing = isCapturing),
            topEndAction = { if (!isCapturing) CardCloseButton(onClick = onDismiss) },
        )
    }

    // 시트는 카드와 다른 창에 뜨므로 캡처한 그림에 섞이지 않는다. 그래도 공유를 시작하면
    // 먼저 닫아, 인스타그램으로 넘어가는 동안 시트가 남지 않게 한다.
    if (isShareSheetVisible) {
        ShareTargetSheet(
            onKakaoTalkClick = {
                onShareSheetDismiss()
                if (!context.shareTextToKakaoTalk(kakaoTalkText)) {
                    onKakaoTalkShareFailed()
                }
            },
            onInstagramStoryClick = {
                onShareSheetDismiss()
                if (!isSharing) {
                    isSharing = true
                    scope.launch {
                        try {
                            val result = shareCardToStory(
                                context = context,
                                capture = capture,
                                setCapturing = { isCapturing = it },
                            )
                            if (result != StoryShareResult.Shared) {
                                onInstagramShareFailed(result)
                            }
                        } finally {
                            isSharing = false
                        }
                    }
                }
            },
            onDismiss = onShareSheetDismiss,
        )
    }
}

/**
 * 카드를 캡처해 인스타그램 스토리로 넘기고 공유 결과를 돌려준다.
 *
 * [setCapturing] 을 켜고 끄는 순서는 [ComposeCapture.captureWith] 가 스스로 지킨다. 캡처가
 * 예외나 시간 초과로 끊겨도 버튼과 닫기 아이콘이 사라진 카드만 남는 상태를 만들지 않는다.
 */
private suspend fun shareCardToStory(
    context: Context,
    capture: ComposeCapture,
    setCapturing: (Boolean) -> Unit,
): StoryShareResult {
    val bitmap = capture.captureWith(setCapturing) ?: return StoryShareResult.ImageUnavailable

    return try {
        context.shareBitmapToInstagramStory(bitmap)
    } finally {
        bitmap.recycle()
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

/** 공유 이미지로 나가는 모양. 닫기 아이콘과 버튼이 빠지고 워드마크가 들어간다. */
@Preview(name = "Share image", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun CardShareImagePreview() {
    GamssTheme(darkTheme = false) {
        GamssEmotionCard(
            date = "26.08.03",
            character = GamssEmotionCardCharacter.ANGER,
            title = "오늘 화~나네",
            description = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이",
            footer = GamssEmotionCardFooter.Brand,
        )
    }
}

@Composable
private fun CardDetailDialogPreviewContent() {
    GamssEmotionCard(
        date = "26.08.03",
        character = GamssEmotionCardCharacter.ANGER,
        title = "오늘 화~나네",
        description = "설느닛람햄을 긱에자네에 신손 겅투히오의 흐랸비의 수매해으는 하어이",
        footer = GamssEmotionCardFooter.Actions(
            primaryLabel = stringResource(R.string.archive_card_discard),
            secondaryLabel = stringResource(R.string.archive_card_view_conversation),
            shareLabel = stringResource(R.string.archive_card_share),
            onPrimaryClick = {},
            onSecondaryClick = {},
            onShareClick = {},
        ),
        topEndAction = { CardCloseButton(onClick = {}) },
    )
}
