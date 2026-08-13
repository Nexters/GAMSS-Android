package com.gamss.android.core.designsystem.checkbox

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 여러 항목을 고르는 자리에서 선택 여부를 나타내는 표시입니다.
 *
 * 모양은 원형입니다. 고르지 않은 상태는 테두리만 있는 링이고, 고른 상태는 채운 원 안에 가로 막대가
 * 뚫린 형태입니다. 두 상태를 각각 내보낸 자산으로 그리고 색만 테마 토큰으로 덮습니다. 자산의 색이
 * gray400 과 red 와 같은 값이라 라이트에서는 그대로이고 다크에서는 함께 반전합니다.
 *
 * 라디오처럼 보이지만 역할은 다중 선택 토글이므로 접근성에는 [Role.Checkbox]로 노출합니다.
 *
 * 카드나 목록 행처럼 부모가 이미 탭을 받는 자리에서는 [onCheckedChange]를 null로 두어 표시 전용으로
 * 사용하세요. 그러면 클릭 대상이 부모 하나로 유지되어, 표시 옆 1dp를 눌렀을 때 동작이 갈리지
 * 않고 접근성 트리에도 같은 동작이 두 번 노출되지 않습니다. 이때 선택 여부는 부모가
 * [androidx.compose.ui.semantics.selected]로 알립니다.
 */
@Composable
fun GamssCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val clickableModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            role = Role.Checkbox,
            indication = null,
            interactionSource = null,
        )
    } else {
        Modifier
    }

    Icon(
        modifier = modifier
            .size(CheckboxSize)
            .then(clickableModifier),
        painter = painterResource(
            if (checked) R.drawable.ic_checkbox_on else R.drawable.ic_checkbox_off,
        ),
        contentDescription = null,
        tint = if (checked) GamssTheme.colors.red else GamssTheme.colors.gray400,
    )
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssCheckboxLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssCheckboxPreviewContent()
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
private fun GamssCheckboxDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssCheckboxPreviewContent()
    }
}

@Composable
private fun GamssCheckboxPreviewContent() {
    Row(
        modifier = Modifier.padding(GamssTheme.spacing.spacing300),
        horizontalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing200),
    ) {
        GamssCheckbox(checked = false)
        GamssCheckbox(checked = true)
        GamssCheckbox(checked = true, onCheckedChange = {})
    }
}

// 자산의 viewBox 가 20 이고 원은 그 안에서 16 이다. 좌우 여백 2 는 자산이 이미 품고 있다.
private val CheckboxSize = 20.dp
