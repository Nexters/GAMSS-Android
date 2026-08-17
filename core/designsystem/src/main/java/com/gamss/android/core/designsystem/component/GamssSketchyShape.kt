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

// Figma 목업의 입력바/감정 패널 테두리는 완전한 직선 사각형이 아니라 손으로 그은 듯 아주
// 은은하게 굽이친다. 네 변을 각각 여러 점으로 쪼개 양방향으로 아주 작게 흔든 다음, 그 점들을
// 매끄러운 곡선으로 잇는다. 매 recomposition 마다 다시 흔들리면 타이핑할 때마다 테두리가
// 꿈틀거려 보이므로, 고정 시드로 항상 같은 흔들림을 재현한다.
// 각 점의 흔들림이 서로 독립적인 난수라 Catmull-Rom 접선이 이웃 점 사이의 편차를 그대로
// 증폭시킨다 - 점 단위 진폭보다 눈에 보이는 물결이 훨씬 커지므로, Figma의 은은한 흔들림에
// 맞추려면 진폭 자체를 훨씬 작게 잡아야 한다.
private val EdgeJitter = 0.35.dp
private const val EDGE_MID_POINTS = 2
private const val DEFAULT_SEED = 20240521

/**
 * Figma의 손그림 선은 자유 경로를 직접 stroke 한다. 선의 중심과 모든 굴곡을 컴포넌트 안에
 * 두어 draw canvas 가장자리에서 잘리지 않도록 한다.
 */
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
    insetPx: Float = 0f,
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

    // 코너 사이를 곡선으로 부드럽게 잇기 위해, 각 변의 점들을 먼저 하나의 목록으로 모은다.
    val points = mutableListOf<Offset>()
    corners.indices.forEach { i ->
        val start = corners[i]
        val end = corners[(i + 1) % corners.size]
        points += start
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy).let { if (it == 0f) 1f else it }
        // 안전 여백 안에서 양방향으로 흔들어 Figma의 손그림 선처럼 보이게 한다.
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

    // Catmull-Rom 곡선을 cubic Bézier로 바꾼다. 제어점만 이용하는 방식과 달리 모든 생성점을
    // 실제로 지나가므로, Figma의 굴곡 강도가 Compose에서 약해지지 않는다.
    for (i in 0 until points.size) {
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
