package com.gamss.android.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.theme.GamssTheme

/** 입력바 아래에 무언가를 띄우는 화면이 있어 공개한다. */
val GamssInputBarHeight = 48.dp

private val InputBarHeight = GamssInputBarHeight
private val InputBarStartPadding = 16.dp

/** 전송 버튼은 배경까지 담긴 32x32 에셋이라 tint 하지 않고 그대로 그린다. */
private val SendButtonSize = 32.dp

private val SendButtonTouchSize = 48.dp
private val InputBarEndPadding = 8.dp

// 답장 미리보기가 없을 때 글과 테두리 사이에 항상 남겨야 하는 여백. 줄 수와 무관하게 고정값으로
// 둬야 두 줄 이상으로 늘어나도 글이 테두리에 붙지 않는다.
private val InputBarVerticalPadding = 8.dp

// 답장 미리보기가 있을 때 쓰는 세로 여백/간격.
private val InputBarReplyVerticalPadding = 14.dp
private val InputBarReplyGap = 14.dp

private val ReplyClearIconSize = 20.dp
private val ReplyClearTouchSize = 32.dp

// 140자 상한을 이 폭에서 담으려면 6줄이면 넉넉하다. 넘치면 입력칸 안에서 스크롤된다.
private const val INPUT_MAX_LINES = 5

@Suppress("LongParameterList")
@Composable
fun GamssInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailingContentDescription: String? = null,
    enabled: Boolean = true,
    /**
     * 전송 버튼(및 키보드 전송 액션)만 따로 잠글 때 쓴다. 기본은 [enabled]를 그대로 따른다.
     * 입력칸은 계속 켜둔 채 전송만 잠깐 막고 싶을 때(예: 메시지 전송 중 포커스·키보드는
     * 유지하되 중복 전송만 막는 경우) [enabled]와 분리해 넘긴다.
     */
    sendEnabled: Boolean = enabled,
    /** 후행 아이콘 앞에 놓이는 자리. 홈은 여기에 감정 선택 토글을 단다. */
    trailingAction: @Composable (() -> Unit)? = null,
    /** 이 줄 수까지 늘어나고, 넘는 내용은 입력칸 안에서 스크롤된다. 화면별 글자 상한에 맞춰 조정한다. */
    maxLines: Int = INPUT_MAX_LINES,
    /** 답장 대상 미리보기. null 이 아니면 입력칸 위에 같은 테두리 안에서 보여준다. */
    replyQuote: ChatReplyQuote? = null,
    onReplyClear: () -> Unit = {},
    replyClearContentDescription: String? = null,
) {
    val canSubmit = sendEnabled && value.isNotBlank()
    val verticalPadding = if (replyQuote != null) InputBarReplyVerticalPadding else 0.dp

    Column(
        modifier = modifier
            .gamssSketchyBox(artRes = R.drawable.bg_textfield_default, background = GamssTheme.colors.white)
            .heightIn(min = GamssInputBarHeight)
            .padding(start = InputBarStartPadding, top = verticalPadding, bottom = verticalPadding),
    ) {
        if (replyQuote != null) {
            ReplyPreviewRow(
                replyQuote = replyQuote,
                onClear = onReplyClear,
                clearContentDescription = replyClearContentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = InputBarStartPadding, bottom = InputBarReplyGap),
            )
        }
        Row(
            modifier = Modifier
                // 전송 버튼 터치 영역(48dp)을 최소치로 잡는다. 140자를 채워 여러 줄이 되면 그
                // 이상으로 늘어난다.
                .heightIn(min = InputBarHeight)
                .padding(end = InputBarEndPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .let { if (replyQuote == null) it.padding(vertical = InputBarVerticalPadding) else it },
                enabled = enabled,
                // 한 줄로 묶으면 140자 상한에 걸려도 가로로 스크롤만 되어 잘린 게 보이지 않는다.
                // 상한을 안 걸면 남은 세로 공간을 전부 채워 버리므로 줄 수로 묶는다.
                singleLine = false,
                minLines = 1,
                maxLines = maxLines,
                textStyle = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray900),
                cursorBrush = SolidColor(GamssTheme.colors.gray900),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSubmit) onTrailingClick() }),
                decorationBox = { innerTextField ->
                    // 자리표시자와 입력칸을 겹쳐 놓아야 한다. 컨테이너 없이 나란히 두면 입력칸이 밀려난다.
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            GamssText(
                                text = placeholder,
                                style = GamssTheme.typography.body4Medium,
                                color = GamssTheme.colors.gray400,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            trailingAction?.invoke()
            Box(
                modifier = Modifier
                    .size(SendButtonTouchSize)
                    .clickable(enabled = canSubmit, role = Role.Button, onClick = onTrailingClick),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(if (canSubmit) GamssIcons.SendButtonOn else GamssIcons.SendButtonOff),
                    contentDescription = trailingContentDescription,
                    modifier = Modifier.size(SendButtonSize),
                )
            }
        }
    }
}

@Composable
private fun ReplyPreviewRow(
    replyQuote: ChatReplyQuote,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    clearContentDescription: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing025),
        ) {
            GamssText(
                text = replyQuote.senderLabel,
                style = GamssTheme.typography.subtitle4,
                color = GamssTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            GamssText(
                text = replyQuote.message,
                style = GamssTheme.typography.body5Medium,
                color = GamssTheme.colors.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(ReplyClearTouchSize)
                .clickable(role = Role.Button, onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(GamssIcons.ClearButton),
                contentDescription = clearContentDescription,
                modifier = Modifier.size(ReplyClearIconSize),
                tint = GamssTheme.colors.gray300,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 402)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssInputBarLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssInputBarPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 402,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssInputBarDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssInputBarPreviewContent()
    }
}

@Composable
private fun GamssInputBarPreviewContent() {
    Column(
        modifier = Modifier.padding(GamssTheme.spacing.spacing400),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing400),
    ) {
        // 빈 상태 — 자리표시자가 보인다.
        GamssInputBar(
            value = "",
            onValueChange = {},
            onTrailingClick = {},
            placeholder = "무슨 이야기를 버려볼까요?",
            modifier = Modifier.fillMaxWidth(),
        )
        // 채워진 상태 — 전송 버튼이 활성화된다.
        GamssInputBar(
            value = "오늘 발표가 너무 떨려요",
            onValueChange = {},
            onTrailingClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        // 비활성 상태 — 입력도, 전송도 막힌다.
        GamssInputBar(
            value = "",
            onValueChange = {},
            onTrailingClick = {},
            placeholder = "지금은 입력할 수 없어요",
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
        // 답장 상태 — 입력칸 위에 답장 미리보기가 같은 테두리 안에서 보인다.
        GamssInputBar(
            value = "",
            onValueChange = {},
            onTrailingClick = {},
            placeholder = "메세지 입력",
            replyQuote = ChatReplyQuote(senderLabel = "불안이에게 답장", message = "안녕"),
            onReplyClear = {},
            replyClearContentDescription = "답장 취소",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
