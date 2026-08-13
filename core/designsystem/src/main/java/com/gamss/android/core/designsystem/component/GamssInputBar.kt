package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

/** 입력바 아래에 무언가를 띄우는 화면이 있어 공개한다. */
val GamssInputBarHeight = 53.dp

private val InputBarHeight = GamssInputBarHeight
private val InputBarBorderWidth = 1.5.dp
private val InputBarStartPadding = 21.dp

/** 전송 버튼은 배경까지 담긴 32x32 에셋이라 tint 하지 않고 그대로 그린다. */
private val SendButtonSize = 32.dp

// 버튼을 48dp 터치 영역으로 감싸면 좌우로 8dp 씩 남는다. 디자인값 18dp 에서 그만큼 빼야
// 버튼의 보이는 가장자리가 18dp 자리에 온다.
private val SendButtonTouchSize = 48.dp
private val InputBarEndPadding = 10.dp

// 140자 상한을 이 폭에서 담으려면 6줄이면 넉넉하다. 넘치면 입력칸 안에서 스크롤된다.
private const val INPUT_MAX_LINES = 6

@Composable
fun GamssInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailingContentDescription: String? = null,
    enabled: Boolean = true,
    /** 후행 아이콘 앞에 놓이는 자리. 홈은 여기에 감정 선택 토글을 단다. */
    trailingAction: @Composable (() -> Unit)? = null,
) {
    val canSubmit = enabled && value.isNotBlank()

    Row(
        modifier = modifier
            // 140자를 채우면 여러 줄이 된다. 한 줄일 때의 높이를 최소치로만 잡고 아래로 늘어나게 둔다.
            .heightIn(min = InputBarHeight)
            .background(GamssTheme.colors.white, RectangleShape)
            .border(InputBarBorderWidth, GamssTheme.colors.black, RectangleShape)
            .padding(start = InputBarStartPadding, end = InputBarEndPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            // 한 줄로 묶으면 140자 상한에 걸려도 가로로 스크롤만 되어 잘린 게 보이지 않는다.
            // 상한을 안 걸면 남은 세로 공간을 전부 채워 버리므로 줄 수로 묶는다.
            singleLine = false,
            minLines = 1,
            maxLines = INPUT_MAX_LINES,
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
                            color = GamssTheme.colors.gray500,
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
