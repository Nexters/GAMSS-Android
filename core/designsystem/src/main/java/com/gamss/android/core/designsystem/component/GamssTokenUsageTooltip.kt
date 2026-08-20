package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.gamssShadow
import com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull
import com.gamss.android.core.designsystem.theme.GamssTheme

private val TooltipWidth = 211.dp
private val TooltipPadding = 12.dp
private val TooltipContentGap = 6.dp
private val TooltipBorderWidth = 1.dp
private val ProgressBarHeight = 9.dp
private val TooltipShadowElevation = 40.dp
private val LoadingIndicatorSize = 24.dp
private val LoadingIndicatorStrokeWidth = 2.dp

/**
 * 상단 내비게이션의 토큰 사용량 아이콘을 눌렀을 때 그 아래 뜨는 툴팁.
 *
 * [usagePercent]는 0~100 사이 값을 그대로 받는다 — 계산(도메인 값 유무 판단 등)은 호출부 책임이다.
 * [isLoading]이 true면 조회 중으로 보고 로딩 인디케이터를 대신 보여준다.
 * 그 외에 null이면 조회에 실패한 것으로 보고 [failMessage]와 재시도 액션([retryLabel]/[onRetryClick])을 대신 보여준다.
 */
@Composable
fun GamssTokenUsageTooltip(
    title: String,
    usagePercent: Int?,
    usagePercentLabel: String,
    resetTimeLabel: String,
    failMessage: String,
    retryLabel: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Column(
        modifier = modifier
            .width(TooltipWidth)
            .background(GamssTheme.colors.white)
            .border(TooltipBorderWidth, GamssTheme.colors.gray950)
            .gamssShadow(shape = RectangleShape, elevation = TooltipShadowElevation)
            .padding(TooltipPadding),
        verticalArrangement = Arrangement.spacedBy(TooltipContentGap),
    ) {
        if (isLoading) {
            Text(
                text = title,
                style = GamssTheme.typography.subtitle4,
                color = GamssTheme.colors.gray900,
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(LoadingIndicatorSize),
                    color = GamssTheme.colors.gray900,
                    strokeWidth = LoadingIndicatorStrokeWidth,
                )
            }
        } else if (usagePercent != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = GamssTheme.typography.subtitle4,
                    color = GamssTheme.colors.gray900,
                )
                Text(
                    text = usagePercentLabel,
                    style = GamssTheme.typography.body4Medium,
                    color = GamssTheme.colors.gray900,
                )
            }

            TokenUsageProgressBar(percent = usagePercent)

            GamssText(
                text = resetTimeLabel,
                style = GamssTheme.typography.body6Regular,
                color = GamssTheme.colors.gray500,
            )
        } else {
            Text(
                text = title,
                style = GamssTheme.typography.subtitle4,
                color = GamssTheme.colors.gray900,
            )
            GamssText(
                text = failMessage,
                style = GamssTheme.typography.body6Regular,
                color = GamssTheme.colors.gray500,
            )
            // 툴팁 톤에 맞춰 큰 CTA 버튼 대신 텍스트 링크 형태로 작게 둔다.
            GamssText(
                text = retryLabel,
                style = GamssTheme.typography.body5Medium,
                color = GamssTheme.colors.gray900,
                modifier = Modifier
                    .align(Alignment.End)
                    .noRippleClickableIfNotNull(onRetryClick),
            )
        }
    }
}

@Composable
private fun TokenUsageProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val fraction = (percent / PERCENT_DENOMINATOR).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ProgressBarHeight)
            .background(GamssTheme.colors.gray075),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(GamssTheme.colors.gray900),
        )
    }
}

private const val PERCENT_DENOMINATOR = 100f

@Preview(name = "TokenUsageTooltip - Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssTokenUsageTooltipPreview() {
    GamssTheme(darkTheme = false) {
        Column {
            GamssTokenUsageTooltip(
                title = "토큰 사용량",
                usagePercent = 60,
                usagePercentLabel = "60% 사용됨",
                resetTimeLabel = "오전 5:00에 초기화됩니다",
                failMessage = "",
                retryLabel = "",
                onRetryClick = {},
            )
            GamssTokenUsageTooltip(
                title = "토큰 사용량",
                usagePercent = null,
                usagePercentLabel = "",
                resetTimeLabel = "",
                failMessage = "사용량을 불러오지 못했어요",
                retryLabel = "재시도",
                onRetryClick = {},
            )
        }
    }
}
