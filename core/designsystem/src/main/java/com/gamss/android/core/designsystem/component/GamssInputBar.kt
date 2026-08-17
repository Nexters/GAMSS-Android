package com.gamss.android.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickable
import com.gamss.android.core.designsystem.theme.GamssTheme

// Figma default(4001:8200) 366x100, focus(3489:4923) 366x149. 둘 다 위 16dp 텍스트 →
// 컨트롤 행 32dp → 아래 16dp 로 구성이 같고, 텍스트에 주는 높이만 다르다.
private val CollapsedInputBarHeight = 100.dp
private val ExpandedInputBarHeight = 149.dp
private val InputBarTopPadding = 16.dp
private val InputBarBottomPadding = 16.dp
private val ControlsRowHeight = 32.dp

// 좌우 여백은 Figma 가 두 상태에서 다르게 잡혀 있다(default 20, focus 16).
private val CollapsedHorizontalPadding = 20.dp
private val ExpandedHorizontalPadding = 16.dp

// 배경까지 담긴 에셋이라 tint 하지 않는다. 컨트롤 행이 32dp 고정이라
// Android 권장 터치 영역(48dp)은 주지 못하고 아이콘 크기가 곧 터치 영역이다.
private val SendButtonSize = 32.dp
private const val INPUT_MAX_LINES = 6
private val InputLineHeight = 20.dp

private const val HEIGHT_ANIMATION_DURATION_MS = 350

/**
 * 텍스트가 위, 감정 선택과 전송 버튼이 아래 행에 놓인다. 포커스가 들어가면 텍스트 자리만 넓어진다.
 *
 * Row/Column 을 갈아 끼우면 BasicTextField 가 새 노드로 다시 만들어져 포커스를 잡자마자 키보드가
 * 닫힌다. 자식은 한 번만 컴포즈하고 배치만 [Layout] 으로 다시 계산하는 이유다.
 */
@Composable
fun GamssInputBar(
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
    var lineCount by remember { mutableIntStateOf(1) }
    var isExpanded by remember { mutableStateOf(false) }

    val baseHeight = if (isExpanded) ExpandedInputBarHeight else CollapsedInputBarHeight
    // 포커스로 기준 높이가 바뀔 때만 애니메이션한다. 줄이 늘어난 만큼은 곧바로 반영해야
    // 방금 친 줄이 애니메이션이 끝날 때까지 잘려 보이지 않는다.
    val animatedBaseHeight by animateDpAsState(
        targetValue = baseHeight,
        animationSpec = tween(durationMillis = HEIGHT_ANIMATION_DURATION_MS),
        label = "GamssInputBarHeight",
    )
    val textAreaHeight =
        animatedBaseHeight - InputBarTopPadding - InputBarBottomPadding - ControlsRowHeight
    val barHeight =
        animatedBaseHeight + (InputLineHeight * lineCount - textAreaHeight).coerceAtLeast(0.dp)

    val horizontalPadding = if (isExpanded) ExpandedHorizontalPadding else CollapsedHorizontalPadding

    Layout(
        modifier = modifier
            .height(barHeight)
            .gamssSketchyBox()
            // 확장 첫 프레임엔 텍스트 자리가 0dp 로 잡혀 컨트롤 행 위로 넘친다.
            .clipToBounds(),
        content = {
            InputTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                placeholder = placeholder,
                canSubmit = canSubmit,
                onSubmit = onSend,
                onFocusChanged = { isExpanded = it },
                onLineCountChanged = { lineCount = it },
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
        val sidePad = horizontalPadding.roundToPx()
        // 폭이 열려 있는 부모(가로 스크롤 등) 아래에서는 maxWidth 가 Infinity 라 그대로 쓰면 터진다.
        val barWidth = constraints.constrainWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth,
        )
        val availableWidth = (barWidth - sidePad * 2).coerceAtLeast(0)
        val totalHeight = constraints.maxHeight
        val topPad = InputBarTopPadding.roundToPx()
        val controlsHeight = ControlsRowHeight.roundToPx()
        val textHeight =
            (totalHeight - topPad - InputBarBottomPadding.roundToPx() - controlsHeight).coerceAtLeast(0)

        val controls = controlsMeasurable.measure(Constraints.fixed(availableWidth, controlsHeight))
        val text = textFieldMeasurable.measure(Constraints.fixed(availableWidth, textHeight))

        layout(barWidth, totalHeight) {
            text.place(sidePad, topPad)
            controls.place(sidePad, topPad + textHeight)
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
    onLineCountChanged: (Int) -> Unit,
) {
    // 입력바 SVG는 라이트 배경이므로 시스템 다크 모드와 무관하게 전경을 검정으로 고정한다.
    val textStyle = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.black)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        enabled = enabled,
        singleLine = false,
        minLines = 1,
        maxLines = INPUT_MAX_LINES,
        onTextLayout = { onLineCountChanged(it.lineCount.coerceIn(1, INPUT_MAX_LINES)) },
        textStyle = textStyle,
        cursorBrush = SolidColor(GamssTheme.colors.black),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (canSubmit) onSubmit() }),
        decorationBox = { innerTextField ->
            // 자리표시자와 입력칸을 겹쳐 놓아야 한다. 컨테이너 없이 나란히 두면 입력칸이 밀려난다.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart,
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
        // 폭이 고정 측정되므로 밀어내지 않으면 전송 버튼이 왼쪽 끝에 붙는다.
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
    Image(
        painter = painterResource(if (canSubmit) GamssIcons.SendButtonOn else GamssIcons.SendButtonOff),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(SendButtonSize)
            .noRippleClickable(enabled = canSubmit, role = Role.Button, onClick = onClick),
    )
}
