package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleClickable
import com.gamss.android.core.designsystem.theme.GamssTheme

private val ToggleMinTouchHeight = 48.dp
private val LabelChevronGap = 4.dp
private val ChevronSize = 16.dp
private const val CHEVRON_ROTATION = 90f

// ic_right_chevron 글리프의 실제 중심. 24 뷰포트에서 stroke 포함 x 12.4~21.3, y 3.9~20.2 다.
// 회전축을 글리프 중심으로 옮기지 않으면 펼칠 때마다 위아래로 튄다.
private const val CHEVRON_CENTER_X = 16.85f / 24f
private const val CHEVRON_CENTER_Y = 12.03f / 24f

/**
 * 라벨과 펼침 상태를 가리키는 셰브론을 나란히 둔 토글. 아래위 셰브론 에셋이 없어 오른쪽 셰브론을 돌려 쓴다.
 *
 * 터치 영역은 [ToggleMinTouchHeight] 까지 넓히지만, 높이가 고정 측정되는 곳에 놓이면 그만큼만 잡힌다.
 * 가로는 라벨과 셰브론 폭만으로 이미 권장치를 넘어, 여백을 더 주면 라벨만 안쪽으로 밀린다.
 */
@Composable
fun GamssDisclosureToggle(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = ToggleMinTouchHeight)
            .noRippleClickable(role = Role.DropdownList, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LabelChevronGap),
    ) {
        GamssText(
            text = label,
            style = GamssTheme.typography.body4Medium,
            // 흰 입력바 위에서 다크 테마의 흰 전경색이 사라지지 않게 Figma 전경색을 유지한다.
            color = GamssTheme.colors.black,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(GamssIcons.RightChevron),
            contentDescription = null,
            tint = GamssTheme.colors.black,
            modifier = Modifier
                .size(ChevronSize)
                .graphicsLayer {
                    rotationZ = if (expanded) -CHEVRON_ROTATION else CHEVRON_ROTATION
                    transformOrigin = TransformOrigin(CHEVRON_CENTER_X, CHEVRON_CENTER_Y)
                },
        )
    }
}
