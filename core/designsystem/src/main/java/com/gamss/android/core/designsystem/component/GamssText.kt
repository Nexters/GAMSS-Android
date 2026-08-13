package com.gamss.android.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.gamss.android.core.designsystem.theme.GamssTheme

/** 앱이 MaterialTheme 을 설정하지 않아 [Text] 를 그냥 쓰면 M3 기본값이 잡힌다. 기본값을 GamssTheme 으로 돌려 둔다. */
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
