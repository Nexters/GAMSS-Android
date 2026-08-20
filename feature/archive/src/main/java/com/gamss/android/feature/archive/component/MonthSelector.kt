package com.gamss.android.feature.archive.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.archive.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.gamss.android.core.designsystem.R as DesignSystemR

@Composable
internal fun MonthSelector(
    yearMonth: YearMonth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                start = SelectorHorizontalMargin,
                end = SelectorHorizontalMargin,
                top = SelectorTopMargin,
            )
            .fillMaxWidth()
            .height(SelectorHeight)
            // 입력바와 같은 손그림 테두리 에셋을 재사용한다.
            .paint(painterResource(DesignSystemR.drawable.bg_input_box), contentScale = ContentScale.FillBounds)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SelectorContentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = yearMonth.format(YearMonthFormatter),
            style = GamssTheme.typography.title4,
            color = GamssTheme.colors.gray900,
        )
        DownChevron(contentDescription = stringResource(R.string.archive_select_month_description))
    }
}

/**
 * 아래 셰브론 에셋이 없어 오른쪽 셰브론을 돌려 쓴다.
 *
 * 글리프가 뷰포트 오른쪽에 몰려 있어 박스 중심을 축으로 돌리면 그 가로 편차가 세로 어긋남이 된다.
 * 회전축을 글리프 중심에 두고, 대신 박스를 넘어가는 잉크만큼 밀어 좌우 여백을 맞춘다.
 */
@Composable
private fun DownChevron(contentDescription: String?) {
    Image(
        painter = painterResource(DesignSystemR.drawable.ic_right_chevron),
        contentDescription = contentDescription,
        modifier = Modifier
            .padding(end = ChevronInkOverflow)
            .size(ChevronSize)
            .graphicsLayer {
                rotationZ = CHEVRON_ROTATION
                transformOrigin = TransformOrigin(CHEVRON_CENTER_X, CHEVRON_CENTER_Y)
            },
    )
}

private const val CHEVRON_ROTATION = 90f

// ic_right_chevron 글리프의 중심. 24 뷰포트에서 stroke 포함 x 12.4~21.3, y 3.9~20.2 다.
private const val CHEVRON_CENTER_X = 16.85f / 24f
private const val CHEVRON_CENTER_Y = 12.03f / 24f

private val YearMonthFormatter = DateTimeFormatter.ofPattern("yyyy.MM")
private val SelectorHorizontalMargin = 18.dp
private val SelectorTopMargin = 24.dp
private val SelectorHeight = 48.dp
private val SelectorContentPadding = 12.dp
private val ChevronSize = 18.dp
private val ChevronInkOverflow = 1.dp
