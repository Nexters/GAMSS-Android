package com.gamss.android.core.designsystem.card

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.core.designsystem.theme.GamssSpacing
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 채움 없이 테두리로만 경계를 만드는 카드입니다.
 *
 * [backgroundColor] 기본값이 white가 아니라 background인 이유가 있습니다. 라이트에서 두 값은 같지만,
 * 다크에서 white로 두면 배경이 흰색인데 테두리로 쓰는 gray950도 함께 밝아져 테두리가 사라집니다.
 * background는 다크에서 검정으로 반전하므로 두 테마 모두에서 테두리가 보입니다.
 *
 * [onClick]과 [onLongClick]이 모두 null이면 클릭 수정자를 아예 붙이지 않습니다. 이유는
 * [com.gamss.android.core.designsystem.modifier.noRippleClickableIfNotNull]과 같습니다.
 */
@Suppress("LongParameterList")
@Composable
fun GamssOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RectangleShape,
    backgroundColor: Color = GamssTheme.colors.background,
    borderColor: Color = GamssTheme.colors.gray950,
    borderWidth: Dp = CardBorderWidth,
    contentPadding: PaddingValues = CardContentPadding,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color = backgroundColor, shape = shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .noRippleCombinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(contentPadding),
        content = content,
    )
}

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssOutlinedCardLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssOutlinedCardPreviewContent()
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
private fun GamssOutlinedCardDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssOutlinedCardPreviewContent()
    }
}

@Composable
private fun GamssOutlinedCardPreviewContent() {
    Column(
        modifier = Modifier.padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
    ) {
        GamssOutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "클릭할 수 없는 카드",
                style = GamssTheme.typography.subtitle4,
                color = GamssTheme.colors.gray900,
            )
        }
        GamssOutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            onLongClick = {},
        ) {
            Text(
                text = "탭과 길게 누르기를 받는 카드",
                style = GamssTheme.typography.subtitle4,
                color = GamssTheme.colors.gray900,
            )
        }
    }
}

private val CardBorderWidth = 1.dp
private val CardContentPadding = PaddingValues(GamssSpacing.spacing400)
