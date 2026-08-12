package com.gamss.android.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 디자인 토큰을 기본값으로 쓰는 텍스트.
 *
 * 앱은 MaterialTheme 을 설정하지 않아 [Text] 를 그냥 쓰면 M3 기본 타이포/색이 잡힌다.
 * 색과 스타일을 늘 함께 넘겨야 하는 실수를 막으려고 기본값을 GamssTheme 쪽으로 돌려 둔다.
 */
@Composable
fun GamssText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = GamssTheme.typography.body3Regular,
    color: Color = GamssTheme.colors.gray900,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}
