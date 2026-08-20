package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.GamssTouchTarget

private val LabelChevronGap = 4.dp
private val ChevronSize = 16.dp
private const val CHEVRON_ROTATION = 90f

// ic_right_chevron 글리프는 24 뷰포트 안에서 치우쳐 있다. 박스 중심으로 돌리면 펼칠 때 위아래로 튄다.
private const val CHEVRON_CENTER_X = 16.85f / 24f
private const val CHEVRON_CENTER_Y = 12.03f / 24f

/** 아래위 셰브론 에셋이 없어 오른쪽 셰브론을 돌려 쓴다. */
@Composable
fun GamssDisclosureToggle(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = GamssTheme.colors.gray900,
) {
    // 잠긴 상태의 회색은 같은 입력바의 placeholder 와 맞춘다. 다크에서도 반전되지 않는 중간 회색이다.
    val resolvedColor = if (enabled) contentColor else GamssTheme.colors.gray500

    Row(
        modifier = modifier
            .heightIn(min = GamssTouchTarget.minimum)
            .noRippleClickable(enabled = enabled, role = Role.DropdownList, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LabelChevronGap),
    ) {
        GamssText(
            text = label,
            style = GamssTheme.typography.body4Medium,
            color = resolvedColor,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(GamssIcons.RightChevron),
            contentDescription = null,
            tint = resolvedColor,
            modifier = Modifier
                .size(ChevronSize)
                .graphicsLayer {
                    rotationZ = if (expanded) -CHEVRON_ROTATION else CHEVRON_ROTATION
                    transformOrigin = TransformOrigin(CHEVRON_CENTER_X, CHEVRON_CENTER_Y)
                },
        )
    }
}
