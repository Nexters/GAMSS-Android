package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R

private val DefaultHighlightHeight = 19.dp

/**
 * 문구 뒤에 형광펜을 그어 둔 것처럼 보이게 한다.
 *
 * 형광펜 높이는 글줄 높이보다 낮아 아래쪽에 붙인다. 텍스트 폭에 맞춰 가로로 늘어난다.
 */
@Composable
fun GamssMarkerHighlight(
    modifier: Modifier = Modifier,
    highlightHeight: Dp = DefaultHighlightHeight,
    content: @Composable BoxScope.() -> Unit,
) {
    val painter = painterResource(R.drawable.img_marker_highlight)

    Box(
        modifier = modifier.drawBehind {
            val markerHeight = highlightHeight.toPx()
            translate(top = size.height - markerHeight) {
                with(painter) {
                    draw(size = Size(this@drawBehind.size.width, markerHeight))
                }
            }
        },
        content = content,
    )
}
