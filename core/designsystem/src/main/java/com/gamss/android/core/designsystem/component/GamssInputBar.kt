package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
            .height(InputBarHeight)
            .background(GamssTheme.colors.white, RectangleShape)
            .border(InputBarBorderWidth, GamssTheme.colors.black, RectangleShape)
            .padding(start = InputBarStartPadding, end = InputBarEndPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            enabled = enabled,
            singleLine = true,
            textStyle = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray900),
            cursorBrush = SolidColor(GamssTheme.colors.gray900),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSubmit) onTrailingClick() }),
            decorationBox = { innerTextField ->
                // 자리표시자와 입력칸을 겹쳐 놓아야 한다. 컨테이너 없이 나란히 두면 입력칸이 밀려난다.
                Box(
                    modifier = Modifier.fillMaxSize(),
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
