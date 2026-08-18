package com.gamss.android.feature.archive.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.feature.archive.PaperFall
import com.gamss.android.feature.archive.R
import com.gamss.android.feature.archive.designScale
import com.gamss.android.feature.archive.designWidth

/** 위에서 쏟아져 바닥에 쌓이는 종이 더미. 어디에 어떻게 놓이는지는 [PaperFall] 이 정한다. */
@Composable
internal fun PaperPile(
    cards: List<CardEntry>,
    onPaperClick: (CardEntry) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = PileTopPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val scale = designScale(maxWidth)
        val density = LocalDensity.current
        val boundsWidthPx = with(density) { designWidth(scale).toPx() }
        val boundsHeightPx = with(density) { maxHeight.toPx() }
        val paperSizePx = with(density) { (PaperSize * scale).toPx() }
        val radiusPx = paperSizePx / 2f * PAPER_COLLISION_RADIUS_SCALE

        // 키에 화면 크기를 넣지 않는다. 회전 등으로 크기만 바뀌었을 때 이미 쌓인 종이가 다시 쏟아진다.
        val fall = remember(cards) {
            PaperFall(cards.size, boundsWidthPx, boundsHeightPx, radiusPx)
        }

        LaunchedEffect(fall) { fall.run() }

        cards.forEachIndexed { index, card ->
            val paper = fall.papers.getOrNull(index) ?: return@forEachIndexed
            Image(
                painter = painterResource(R.drawable.archive_paper),
                contentDescription = stringResource(
                    R.string.archive_paper_description,
                    card.date.monthValue,
                    card.date.dayOfMonth,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(PaperSize * scale)
                    // 터치 영역도 그려진 자리를 따라가야 하므로 clickable 을 레이어 안쪽에 둔다.
                    .graphicsLayer {
                        translationX = paper.x - paperSizePx / 2f
                        translationY = paper.y - paperSizePx / 2f
                        rotationZ = paper.rotationDegrees
                    }
                    .clickable(role = Role.Button) { onPaperClick(card) },
            )
        }
    }
}

/** 종이는 네모지만 충돌은 원으로 근사한다. 모서리까지 덮으려 반지름을 조금 키운다. */
private const val PAPER_COLLISION_RADIUS_SCALE = 1.15f

private val PileTopPadding = 136.dp
private val PaperSize = 88.dp
