package com.gamss.android.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme
import kotlin.math.abs

// 실제 높이는 collapsed 64dp <-> expanded 149dp+ 로 변하므로, 바깥에서는 측정값을 써야 한다.
private val GamssInputBarHeight = 64.dp

private val InputBarStartPadding = 16.dp
private val InputBarEndPadding = 16.dp
private val ControlsRowGap = 4.dp

/** 전송 버튼은 배경까지 담긴 32x32 에셋이라 tint 하지 않고 그대로 그린다. */
private val SendButtonSize = 32.dp

// expanded 컨트롤 행이 32dp 고정이라 Android 권장 터치 영역(48dp)을 못 주고 아이콘 크기에 맞춘다.
private val SendButtonTouchSize = 32.dp
private const val INPUT_MAX_LINES = 6
private val InputLineHeight = 20.dp

// Figma(node 3483:16376): 포커스가 들어가면 입력창이 366x149로 커지고, 텍스트는 위(16dp 여백)에
// 남고 감정 선택·전송 버튼은 하단(16dp 여백)의 별도 행으로 내려간다.
private val ExpandedInputBarHeight = 149.dp
private val ExpandedTopPadding = 16.dp
private val ExpandedBottomPadding = 16.dp
private val ControlsRowHeight = 32.dp

// 149 - 위16 - 아래16 - 컨트롤행32 = 85dp: 컨트롤 행과 겹치지 않고 텍스트가 늘어날 수 있는 최대 높이.
private val ExpandedTextAreaMaxHeight =
    ExpandedInputBarHeight - ExpandedTopPadding - ExpandedBottomPadding - ControlsRowHeight

// 기본 스프링(약 300ms)은 순간적으로 튀는 느낌이라, 손그림 톤에 맞게 좀 더 여유 있게 늘어나도록 늦춘다.
private const val HEIGHT_ANIMATION_DURATION_MS = 350

// 애니메이션 도중에만 쓰는 프로시저럴 테두리 굵기. 4배 에셋에서 실측한 6px = 1.5dp 에 맞춘다.
private val InputBarBorderWidth = 1.5.dp

/**
 * 포커스 여부로 collapsed(한 줄) <-> expanded(텍스트 위 / 컨트롤 아래) 모양이 바뀐다.
 *
 * Row/Column 을 if-else 로 갈아 끼우면 BasicTextField 가 매번 새 노드로 다시 만들어져 포커스를
 * 잡자마자 키보드가 닫힌다. 그래서 자식은 한 번만 컴포즈하고 배치만 커스텀 [Layout] 로 매 프레임
 * 다시 계산한다.
 */
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
    var lineCount by remember { mutableIntStateOf(1) }
    var isExpanded by remember { mutableStateOf(false) }

    val collapsedHeight = GamssInputBarHeight + InputLineHeight * (lineCount - 1)
    val expandedHeight = ExpandedInputBarHeight +
        (InputLineHeight * lineCount - ExpandedTextAreaMaxHeight).coerceAtLeast(0.dp)
    val targetHeight = if (isExpanded) expandedHeight else collapsedHeight
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = HEIGHT_ANIMATION_DURATION_MS),
        label = "GamssInputBarHeight",
    )

    // collapsed/expanded 에셋은 세로 비율이 달라 중간 높이로 늘리면 테두리 굵기가 왜곡된다
    // (손그림이라 9-patch도 못 쓴다). 정착했을 때만 실제 에셋을 쓴다.
    val isAnimating = abs((animatedHeight - targetHeight).value) > 0.01f
    val useFocusArt = animatedHeight > (collapsedHeight + expandedHeight) / 2

    Layout(
        modifier = modifier
            .height(animatedHeight)
            .inputBarBackground(useFocusArt = useFocusArt, isAnimating = isAnimating),
        content = {
            InputTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                placeholder = placeholder,
                canSubmit = canSubmit,
                onSubmit = onTrailingClick,
                alignment = if (isExpanded) Alignment.TopStart else Alignment.CenterStart,
                onFocusChanged = { isExpanded = it },
                onLineCountChanged = { lineCount = it },
            )
            ControlsRow(
                isExpanded = isExpanded,
                canSubmit = canSubmit,
                trailingContentDescription = trailingContentDescription,
                onTrailingClick = onTrailingClick,
                trailingAction = trailingAction,
            )
        },
    ) { measurables, constraints ->
        val (textFieldMeasurable, controlsMeasurable) = measurables
        val startPad = InputBarStartPadding.roundToPx()
        val endPad = InputBarEndPadding.roundToPx()
        val availableWidth = (constraints.maxWidth - startPad - endPad).coerceAtLeast(0)
        val totalHeight = constraints.maxHeight

        if (isExpanded) {
            val topPad = ExpandedTopPadding.roundToPx()
            val bottomPad = ExpandedBottomPadding.roundToPx()
            val controlsHeight = ControlsRowHeight.roundToPx()
            val controlsPlaceable = controlsMeasurable.measure(
                Constraints.fixed(availableWidth, controlsHeight),
            )
            val textHeight = (totalHeight - topPad - bottomPad - controlsHeight).coerceAtLeast(0)
            val textPlaceable = textFieldMeasurable.measure(
                Constraints.fixed(availableWidth, textHeight),
            )
            layout(constraints.maxWidth, totalHeight) {
                textPlaceable.place(startPad, topPad)
                controlsPlaceable.place(startPad, topPad + textHeight)
            }
        } else {
            val controlsPlaceable = controlsMeasurable.measure(
                Constraints(maxWidth = availableWidth, maxHeight = totalHeight),
            )
            val textWidth = (availableWidth - controlsPlaceable.width).coerceAtLeast(0)
            val textPlaceable = textFieldMeasurable.measure(
                Constraints.fixed(textWidth, totalHeight),
            )
            layout(constraints.maxWidth, totalHeight) {
                textPlaceable.place(startPad, 0)
                controlsPlaceable.place(startPad + textPlaceable.width, (totalHeight - controlsPlaceable.height) / 2)
            }
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
    alignment: Alignment,
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
                contentAlignment = alignment,
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
    isExpanded: Boolean,
    canSubmit: Boolean,
    trailingContentDescription: String?,
    onTrailingClick: () -> Unit,
    trailingAction: @Composable (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        trailingAction?.invoke()
        if (trailingAction != null) {
            // collapsed 에서는 전송 버튼 바로 앞에 좁은 간격만, expanded 에서는 양 끝으로 벌어진다.
            if (isExpanded) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.width(ControlsRowGap))
            }
        }
        SendButton(canSubmit = canSubmit, contentDescription = trailingContentDescription, onClick = onTrailingClick)
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
            .size(SendButtonTouchSize)
            .clickable(enabled = canSubmit, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (canSubmit) GamssIcons.SendButtonOn else GamssIcons.SendButtonOff),
            contentDescription = contentDescription,
            modifier = Modifier.size(SendButtonSize),
        )
    }
}

/** 4배 해상도로 export한 손그림 테두리(collapsed 366x64 / expanded 368x152). 애니메이션 중엔
 * [gamssSketchyBorder] 로 대신 그리고, 정착한 뒤에만 실제 에셋을 쓴다. */
@Composable
private fun Modifier.inputBarBackground(useFocusArt: Boolean, isAnimating: Boolean): Modifier =
    if (isAnimating) {
        this.background(GamssTheme.colors.white)
            .gamssSketchyBorder(color = GamssTheme.colors.black, width = InputBarBorderWidth)
    } else {
        this.paint(
            painter = painterResource(if (useFocusArt) R.drawable.bg_input_box_focus else R.drawable.bg_input_box),
            contentScale = ContentScale.FillBounds,
        )
    }
