package com.gamss.android.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

private val ArtCorner = 12.dp

// 아트의 모서리가 둥글어, 배경을 직각으로 칠하면 흰 모서리가 선 밖으로 삐져나온다.
private val BackgroundCorner = 4.dp

private val NineSliceCells = (0..2).flatMap { col -> (0..2).map { row -> col to row } } - (1 to 1)

/**
 * 손그림 테두리 아트를 9-slice 로 깐다. 모서리는 원본 크기로 두고 직선 구간만 늘려,
 * 어떤 크기에서도 선 굵기가 변하지 않는다.
 *
 * 아트는 호출부가 명시한다. 기본값을 두면 다른 기능이 같은 drawable 을 손볼 때
 * 무엇이 함께 바뀌는지 드러나지 않는다. 흰 배경은 여기서 칠하므로 아트에는 없어야 한다.
 */
@Composable
fun Modifier.gamssSketchyBox(
    @DrawableRes artRes: Int,
    background: Color = GamssTheme.colors.white,
): Modifier {
    val art = painterResource(artRes)
    return this
        .background(background, RoundedCornerShape(BackgroundCorner))
        .drawBehind { drawNineSlice(art) }
}

private fun DrawScope.drawNineSlice(art: Painter) {
    val artSize = art.intrinsicSize
    if (artSize.isUnspecified) return
    val corner = ArtCorner.toPx()

    val srcX = floatArrayOf(0f, corner, artSize.width - corner, artSize.width)
    val srcY = floatArrayOf(0f, corner, artSize.height - corner, artSize.height)
    val dstX = floatArrayOf(0f, corner, size.width - corner, size.width)
    val dstY = floatArrayOf(0f, corner, size.height - corner, size.height)

    NineSliceCells.forEach { (col, row) ->
        val scaleX = sliceScale(srcX, dstX, col)
        val scaleY = sliceScale(srcY, dstY, row)
        if (scaleX <= 0f || scaleY <= 0f) return@forEach

        clipRect(dstX[col], dstY[row], dstX[col + 1], dstY[row + 1]) {
            translate(dstX[col] - srcX[col] * scaleX, dstY[row] - srcY[row] * scaleY) {
                scale(scaleX, scaleY, pivot = Offset.Zero) {
                    with(art) { draw(artSize) }
                }
            }
        }
    }
}

private fun sliceScale(src: FloatArray, dst: FloatArray, index: Int): Float {
    val srcLength = src[index + 1] - src[index]
    val dstLength = dst[index + 1] - dst[index]
    return if (srcLength <= 0f || dstLength <= 0f) 0f else dstLength / srcLength
}
