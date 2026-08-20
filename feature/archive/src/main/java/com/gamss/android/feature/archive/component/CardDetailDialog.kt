package com.gamss.android.feature.archive.component

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamss.android.core.designsystem.card.GamssEmotionCard
import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.core.designsystem.card.GamssEmotionCardFooter
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.GamssTouchTarget
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
import java.time.format.DateTimeFormatter

/** 배경 dim 은 [Dialog] 창이 기본으로 그려 주므로 여기서 따로 그리지 않는다. */
@Composable
internal fun CardDetailDialog(
    card: Card,
    onDismiss: () -> Unit,
    onDiscardClick: () -> Unit,
    onViewConversationClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val capture = rememberComposeCapture()
    val instagramUnavailableMessage = stringResource(R.string.archive_card_share_instagram_unavailable)
    val imageUnavailableMessage = stringResource(R.string.archive_card_share_image_failed)
    val kakaoTalkUnavailableMessage = stringResource(R.string.archive_card_share_kakaotalk_unavailable)
    val kakaoTalkText = stringResource(
        R.string.archive_card_share_kakaotalk_text,
        stringResource(card.character.cardTitleRes()),
        CardShareLink.of(card.id),
    )

    // 공유 이미지에는 누를 수 없는 버튼과 닫기 아이콘을 남기지 않는다. 감추는 동안만 켜 두고,
    // 그 상태가 실제로 그려진 뒤에 캡처한다.
    var isCapturing by remember { mutableStateOf(false) }
    var isShareSheetVisible by remember { mutableStateOf(false) }

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
                        onShareClick = { isShareSheetVisible = true },
                    )
                },
                modifier = Modifier.captureTo(capture),
                topEndAction = { if (!isCapturing) CardCloseButton(onClick = onDismiss) },
            )
        }

        // 시트는 카드와 다른 창에 뜨므로 캡처한 그림에 섞이지 않는다. 그래도 공유를 시작하면
        // 먼저 닫아, 인스타그램으로 넘어가는 동안 시트가 남지 않게 한다.
        if (isShareSheetVisible) {
            ShareTargetSheet(
                onKakaoTalkClick = {
                    isShareSheetVisible = false
                    if (!context.shareTextToKakaoTalk(kakaoTalkText)) {
                        Toast.makeText(context, kakaoTalkUnavailableMessage, Toast.LENGTH_LONG).show()
                    }
                },
                onInstagramStoryClick = {
                    isShareSheetVisible = false
                    scope.launch {
                        val failure = shareCardToStory(
                            context = context,
                            capture = capture,
                            instagramUnavailableMessage = instagramUnavailableMessage,
                            imageUnavailableMessage = imageUnavailableMessage,
                            setCapturing = { isCapturing = it },
                        )
                        failure?.let { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { isShareSheetVisible = false },
            )
        }
    }
}

/**
 * 카드를 캡처해 인스타그램 스토리로 넘긴다. 실패하면 사용자에게 보일 말을, 성공하면 `null` 을 준다.
 *
 * [setCapturing] 은 try/finally 로 되돌린다. 캡처가 예외나 시간 초과로 끊겨도 버튼과 닫기
 * 아이콘이 사라진 카드만 남는 상태를 만들지 않으려는 것이다.
 */
private suspend fun shareCardToStory(
    context: Context,
    capture: ComposeCapture,
    instagramUnavailableMessage: String,
    imageUnavailableMessage: String,
    setCapturing: (Boolean) -> Unit,
): String? {
    val bitmap = try {
        setCapturing(true)
        capture.captureAfterNextDraw()
    } finally {
        setCapturing(false)
    }
    if (bitmap == null) return imageUnavailableMessage

    return try {
        when (context.shareBitmapToInstagramStory(bitmap)) {
            StoryShareResult.Shared -> null
            StoryShareResult.InstagramUnavailable -> instagramUnavailableMessage
            StoryShareResult.ImageUnavailable -> imageUnavailableMessage
        }
    } finally {
        bitmap.recycle()
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
