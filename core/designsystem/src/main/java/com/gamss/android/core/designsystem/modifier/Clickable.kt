package com.gamss.android.core.designsystem.modifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role

/** 기본 indication 을 두면 손그림 배경 위에 회색 사각형이 덧그려집니다. */
fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = clickable(
    enabled = enabled,
    role = role,
    onClick = onClick,
    indication = null,
    interactionSource = null,
)

/**
 * [onClick]이 null이 아닐 때만, ripple 없이 [Modifier.clickable]을 적용합니다.
 *
 * 클릭 핸들러가 없는 항목에 `enabled = false`로 clickable을 붙이면 접근성 트리에는 여전히
 * "비활성화된 클릭 가능 요소"로 노출됩니다. 클릭 자체가 불가능한 정보성 항목이라면 clickable을
 * 아예 붙이지 않는 것이 의미상 정확하므로 이 확장 함수를 사용합니다.
 */
fun Modifier.noRippleClickableIfNotNull(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) {
        clickable(
            onClick = onClick,
            indication = null,
            interactionSource = null,
        )
    } else {
        this
    }

/**
 * 짧은 탭과 길게 누르기를 ripple 없이 함께 받습니다. 둘 다 null이면 clickable을 붙이지 않습니다.
 *
 * 이유는 [noRippleClickableIfNotNull]과 같습니다. 길게 누르기만 있는 경우에도
 * [Modifier.combinedClickable]이 짧은 탭 핸들러를 요구하므로 빈 람다를 넘깁니다. 이때 탭은 아무
 * 일도 하지 않지만, 길게 누를 수 있는 요소라는 사실은 접근성 트리에 남는 것이 의도입니다.
 */
fun Modifier.noRippleCombinedClickable(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
): Modifier =
    if (onClick == null && onLongClick == null) {
        this
    } else {
        combinedClickable(
            onClick = onClick ?: {},
            onLongClick = onLongClick,
            indication = null,
            interactionSource = null,
        )
    }

// 외부를 클릭할때 포커스를 제거함
@Composable
fun Modifier.dismissOnTapOutside(onDismiss: (() -> Unit)? = null): Modifier {
    val focusManager = LocalFocusManager.current
    // clickable 을 쓰면 레이블 없는 클릭 노드가 화면 전체 크기로 시맨틱 트리에 들어간다.
    return pointerInput(onDismiss) {
        detectTapGestures {
            focusManager.clearFocus()
            if(onDismiss != null) {
                onDismiss()
            }
        }
    }
}