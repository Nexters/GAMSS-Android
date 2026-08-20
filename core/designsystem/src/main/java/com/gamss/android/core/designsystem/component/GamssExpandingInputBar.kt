package com.gamss.android.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.modifier.noRippleClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.GamssTouchTarget

// Figma default 4001:8200, focus 3489:4923.
private val CollapsedInputBarHeight = 112.dp
private val ExpandedInputBarHeight = 150.dp
private val InputBarTopPadding = 16.dp
private val InputBarBottomPadding = 16.dp
private val ControlsRowHeight = 32.dp

private val InputBarHorizontalPadding = 20.dp

private val SendButtonSize = 32.dp

private val ControlsTouchOverhang = (GamssTouchTarget.minimum - ControlsRowHeight) / 2

// 140자 상한을 이 폭에서 담으려면 6줄이면 넉넉하다. 넘치면 입력칸 안에서 스크롤된다.
private const val INPUT_MAX_LINES = 6

private const val HEIGHT_ANIMATION_DURATION_MS = 350

/**
 * 포커스에 따라 높이가 늘어나는 손그림 입력창. 홈이 쓴다.
 * 평면 테두리에 답장 미리보기를 얹는 채팅 입력창은 [GamssInputBar] 쪽이다.
 *
 * Row/Column 을 갈아 끼우면 BasicTextField 가 다시 만들어져 포커스를 잡자마자 키보드가 닫힌다.
 */
@Composable
fun GamssExpandingInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    sendContentDescription: String? = null,
    enabled: Boolean = true,
    beforeSendSlot: @Composable (() -> Unit)? = null,
) {
    val canSubmit = enabled && value.isNotBlank()
    var isExpanded by remember { mutableStateOf(false) }

    val baseHeight = if (isExpanded) ExpandedInputBarHeight else CollapsedInputBarHeight
    // 포커스로 바뀌는 기준 높이만 애니메이션한다. 늘어난 줄까지 태우면 방금 친 줄이 끝날 때까지 잘려 보인다.
    val animatedBaseHeight by animateDpAsState(
        targetValue = baseHeight,
        animationSpec = tween(durationMillis = HEIGHT_ANIMATION_DURATION_MS),
        label = "GamssExpandingInputBarHeight",
    )

    Layout(
        // 두 상태의 손그림이 별개로 그려져 있어 아트도 함께 갈아 끼운다.
        modifier = modifier.gamssSketchyBox(
            artRes = if (isExpanded) R.drawable.bg_expanding_input_box_focus else R.drawable.bg_expanding_input_box,
        ),
        content = {
            InputTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                placeholder = placeholder,
                canSubmit = canSubmit,
                onSubmit = onSend,
                onFocusChanged = { isExpanded = it },
            )
            ControlsRow(
                canSubmit = canSubmit,
                sendContentDescription = sendContentDescription,
                onSend = onSend,
                beforeSendSlot = beforeSendSlot,
            )
        },
    ) { measurables, constraints ->
        val (textFieldMeasurable, controlsMeasurable) = measurables
        val sidePad = InputBarHorizontalPadding.roundToPx()
        // 폭이 열려 있는 부모(가로 스크롤 등) 아래에서는 maxWidth 가 Infinity 라 그대로 쓰면 터진다.
        val barWidth = constraints.constrainWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth,
        )
        val availableWidth = (barWidth - sidePad * 2).coerceAtLeast(0)
        val topPad = InputBarTopPadding.roundToPx()
        val bottomPad = InputBarBottomPadding.roundToPx()
        val controlsHeight = ControlsRowHeight.roundToPx()

        // 기준 높이는 텍스트 자리의 최소값으로만 쓴다. 줄이 넘치면 텍스트가 재는 만큼 바가 자란다.
        val minTextHeight =
            (animatedBaseHeight.roundToPx() - topPad - bottomPad - controlsHeight).coerceAtLeast(0)
        val text = textFieldMeasurable.measure(
            Constraints(minWidth = availableWidth, maxWidth = availableWidth, minHeight = minTextHeight),
        )
        val overhang = ControlsTouchOverhang.roundToPx()
        val controls = controlsMeasurable.measure(
            Constraints.fixed(availableWidth + overhang, GamssTouchTarget.minimum.roundToPx()),
        )

        val barHeight = constraints.constrainHeight(topPad + text.height + controlsHeight + bottomPad)

        layout(barWidth, barHeight) {
            text.place(sidePad, topPad)
            controls.place(sidePad, barHeight - bottomPad - controlsHeight - overhang)
        }
    }
}

@Composable
private fun InputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    val textStyle = GamssTheme.typography.body4Medium
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        enabled = enabled,
        singleLine = false,
        minLines = 1,
        maxLines = INPUT_MAX_LINES,
        // 입력바 SVG는 라이트 배경이므로 시스템 다크 모드와 무관하게 전경을 검정으로 고정한다.
        textStyle = textStyle.copy(color = GamssTheme.colors.black),
        cursorBrush = SolidColor(GamssTheme.colors.black),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (canSubmit) onSubmit() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    GamssText(
                        text = placeholder,
                        style = textStyle,
                        color = GamssTheme.colors.gray500,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ControlsRow(
    canSubmit: Boolean,
    sendContentDescription: String?,
    onSend: () -> Unit,
    beforeSendSlot: @Composable (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        beforeSendSlot?.invoke()
        Spacer(modifier = Modifier.weight(1f))
        SendButton(canSubmit = canSubmit, contentDescription = sendContentDescription, onClick = onSend)
    }
}

@Composable
private fun SendButton(
    canSubmit: Boolean,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(GamssTouchTarget.minimum)
            .noRippleClickable(enabled = canSubmit, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (canSubmit) GamssIcons.SendButtonOn else GamssIcons.SendButtonOff),
            contentDescription = contentDescription,
            modifier = Modifier.size(SendButtonSize),
        )
    }
}
