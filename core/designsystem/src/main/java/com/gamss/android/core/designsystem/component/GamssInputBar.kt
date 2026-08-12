package com.gamss.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

private val InputBarHeight = 53.dp
private val InputBarBorderWidth = 1.5.dp
private val InputBarStartPadding = 21.dp

// 휴지통 버튼은 안쪽 여백만큼 터치 영역이 넓어진다. 오른쪽 여백은 디자인값 18dp 에서 그만큼 뺀다.
private val TrailingHitPadding = 10.dp
private val InputBarEndPadding = 8.dp

/**
 * 걱정을 적어 던지는 한 줄 입력바.
 *
 * 후행 아이콘은 전송 버튼이라, 입력이 비어 있으면 [onTrailingClick] 을 흘려보내지 않고 비활성으로 보여 준다.
 */
@Composable
fun GamssInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailingContentDescription: String? = null,
    @DrawableRes trailingIconRes: Int = GamssIcons.ThrowAway,
    enabled: Boolean = true,
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
            // 입력칸이 바 전체를 차지해야 어디를 눌러도 커서가 잡힌다.
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
        GamssIconButton(
            iconRes = trailingIconRes,
            contentDescription = trailingContentDescription,
            onClick = { if (canSubmit) onTrailingClick() },
            hitPadding = TrailingHitPadding,
            tint = if (canSubmit) GamssTheme.colors.gray900 else GamssTheme.colors.gray300,
        )
    }
}
