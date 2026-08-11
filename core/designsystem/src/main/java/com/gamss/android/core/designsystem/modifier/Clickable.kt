package com.gamss.android.core.designsystem.modifier

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/**
 * [onClick]이 null이 아닐 때만, ripple 없이 [Modifier.clickable]을 적용합니다.
 *
 * 클릭 핸들러가 없는 항목에 `enabled = false`로 clickable을 붙이면 접근성 트리에는 여전히
 * "비활성화된 클릭 가능 요소"로 노출됩니다. 클릭 자체가 불가능한 정보성 항목이라면 clickable을
 * 아예 붙이지 않는 것이 의미상 정확하므로 이 확장 함수를 사용합니다.
 */
fun Modifier.noRippleClickableIfNotNull(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) clickable(
        onClick = onClick,
        indication = null,
        interactionSource = null
    ) else this
