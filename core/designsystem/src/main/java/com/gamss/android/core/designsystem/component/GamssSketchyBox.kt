package com.gamss.android.core.designsystem.component

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
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

// 모서리는 늘리지 않고 원본 크기로 찍어 낸다. 굴곡이 가장 심한 구간이라 넉넉히 잡는다.
private val ArtCorner = 12.dp

// 아트의 모서리가 둥글어, 배경을 직각으로 칠하면 흰 모서리가 선 밖으로 삐져나온다.
private val BackgroundCorner = 4.dp

// 링 안쪽(가운데 칸)은 비어 있어 그릴 것이 없다. 그리기마다 새로 만들지 않게 한 번만 계산한다.
private val NineSliceCells = (0..2).flatMap { col -> (0..2).map { row -> col to row } } - (1 to 1)

/**
 * 손그림 사각 테두리를 9-slice 로 그린다. 모서리는 원본 크기로 두고 직선 구간만 늘리므로,
 * 어떤 크기에 올려도 선 굵기가 변하지 않는다. 통째로 늘리면 늘린 방향의 선만 두꺼워진다.
 */
@Composable
fun Modifier.gamssSketchyBox(background: Color = GamssTheme.colors.white): Modifier {
    val art = painterResource(R.drawable.bg_input_box)
    return this
        .background(background, RoundedCornerShape(BackgroundCorner))
        .drawBehind { drawNineSlice(art) }
}

private fun DrawScope.drawNineSlice(art: Painter) {
    // 에셋 크기를 상수로 복제하면 에셋을 갈아 끼울 때 조용히 어긋난다.
    val artSize = art.intrinsicSize
    if (artSize.isUnspecified) return
    val corner = ArtCorner.toPx()

    // 세 구간의 경계: 원본과 대상 각각. 가운데 구간만 늘어난다.
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

/** 대상이 원본보다 작아 구간이 뒤집히면 0 을 돌려 그리지 않게 한다. */
private fun sliceScale(src: FloatArray, dst: FloatArray, index: Int): Float {
    val srcLength = src[index + 1] - src[index]
    val dstLength = dst[index + 1] - dst[index]
    return if (srcLength <= 0f || dstLength <= 0f) 0f else dstLength / srcLength
}
