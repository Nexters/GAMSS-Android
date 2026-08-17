package com.gamss.android.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.random.Random

// Catmull-Rom 접선이 이웃 점 사이 편차를 증폭시켜, 보이는 물결은 점 단위 진폭보다 훨씬 커진다.
private val EdgeJitter = 0.35.dp
private const val EDGE_MID_POINTS = 2

// 매 recomposition 마다 다시 흔들리면 타이핑할 때마다 테두리가 꿈틀거린다.
private const val DEFAULT_SEED = 20240521

/** 손그림 느낌의 사각 테두리. 선 굵기와 굴곡을 모두 컴포넌트 안에 두어 가장자리에서 잘리지 않는다. */
fun Modifier.gamssSketchyBorder(
    color: Color,
    width: Dp,
    edgeJitter: Dp = EdgeJitter,
    seed: Int = DEFAULT_SEED,
): Modifier = drawWithCache {
    val strokeWidth = width.toPx()
    val path = buildSketchyRectPath(
        size = size,
        edgeJitterPx = edgeJitter.toPx(),
        random = Random(seed),
        insetPx = strokeWidth / 2f,
    )
    onDrawWithContent {
        drawContent()
        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
    }
}

private fun buildSketchyRectPath(
    size: Size,
    edgeJitterPx: Float,
    random: Random,
    insetPx: Float,
): Path {
    val strokeInset = insetPx.coerceAtLeast(0f)
    // 선의 반 폭과 진폭을 모두 확보해야 양방향 굴곡도 캔버스 밖으로 나가지 않는다.
    val safeInset = strokeInset + edgeJitterPx
    fun jitter(amount: Float) = (random.nextFloat() - 0.5f) * 2f * amount
    val corners = listOf(
        Offset(safeInset, safeInset),
        Offset(size.width - safeInset, safeInset),
        Offset(size.width - safeInset, size.height - safeInset),
        Offset(safeInset, size.height - safeInset),
    )

    val points = mutableListOf<Offset>()
    corners.indices.forEach { i ->
        val start = corners[i]
        val end = corners[(i + 1) % corners.size]
        points += start
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy).let { if (it == 0f) 1f else it }
        val nx = -dy / len
        val ny = dx / len
        for (m in 1..EDGE_MID_POINTS) {
            val t = m / (EDGE_MID_POINTS + 1f)
            val amount = jitter(edgeJitterPx)
            points += Offset(start.x + dx * t + nx * amount, start.y + dy * t + ny * amount)
        }
    }

    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    fun Offset.clampToOutline() = Offset(
        x = x.coerceIn(strokeInset, size.width - strokeInset),
        y = y.coerceIn(strokeInset, size.height - strokeInset),
    )

    // Catmull-Rom: 생성점을 모두 지나가므로 굴곡 강도가 약해지지 않는다.
    for (i in points.indices) {
        val previous = points[(i - 1 + points.size) % points.size]
        val current = points[i]
        val next = points[(i + 1) % points.size]
        val following = points[(i + 2) % points.size]
        val firstControl = Offset(
            current.x + (next.x - previous.x) / 6f,
            current.y + (next.y - previous.y) / 6f,
        ).clampToOutline()
        val secondControl = Offset(
            next.x - (following.x - current.x) / 6f,
            next.y - (following.y - current.y) / 6f,
        ).clampToOutline()
        path.cubicTo(
            firstControl.x,
            firstControl.y,
            secondControl.x,
            secondControl.y,
            next.x,
            next.y,
        )
    }
    path.close()

    return path
}
