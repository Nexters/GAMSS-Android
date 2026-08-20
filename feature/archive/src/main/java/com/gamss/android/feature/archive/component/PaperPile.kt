package com.gamss.android.feature.archive.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.gamss.android.core.designsystem.theme.designScale
import com.gamss.android.core.designsystem.theme.designWidth
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.feature.archive.PAPER_COLLISION_RADIUS_SCALE
import com.gamss.android.feature.archive.PaperFall
import com.gamss.android.feature.archive.PaperGeometry
import com.gamss.android.feature.archive.PaperPileTopPadding
import com.gamss.android.feature.archive.PaperSize
import com.gamss.android.feature.archive.R
import java.time.LocalDate

/**
 * 위에서 쏟아져 바닥에 쌓이는 종이 더미. 어디에 어떻게 놓이는지는 [PaperFall] 이 정한다.
 *
 * @param droppedCardDate 방금 버려서 이 화면으로 넘어온 카드의 날짜. 그 한 장만 떨어지고 나머지는
 *  이미 쌓인 채로 시작한다. null 이면 전부 쏟는다.
 */
@Composable
internal fun PaperPile(
    cards: List<CardEntry>,
    droppedCardDate: LocalDate?,
    onPaperClick: (CardEntry) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = PaperPileTopPadding),
    ) {
        val scale = designScale(maxWidth)
        val density = LocalDensity.current
        val paperSizePx = with(density) { (PaperSize * scale).toPx() }
        val geometry = PaperGeometry(
            boundsWidthPx = with(density) { designWidth(scale).toPx() },
            boundsHeightPx = with(density) { maxHeight.toPx() },
            radiusPx = paperSizePx / 2f * PAPER_COLLISION_RADIUS_SCALE,
        )
        // 시뮬레이션은 시안 폭(402dp) 안에서만 돈다. 더 넓은 화면에서는 그 폭을 가운데로 밀어야
        // 상단바·월 셀렉터와 축이 맞는다. 종이는 TopStart 기준이라 contentAlignment 로는 안 된다.
        val pileOffsetX = with(density) { ((maxWidth - designWidth(scale)) / 2).toPx() }

        // 키에 화면 크기를 넣지 않는다. 크기만 바뀌었을 때 이미 쌓인 종이가 다시 쏟아지면 안 된다.
        // 바뀐 칸은 컴포지션이 확정된 뒤에 흘려 넣는다. 버려질 수 있는 컴포지션에서 쓰면 안 된다.
        val fall = remember(cards, droppedCardDate) {
            PaperFall(
                count = cards.size,
                geometry = geometry,
                droppingIndex = cards.droppedIndex(droppedCardDate),
            )
        }
        SideEffect { fall.updateGeometry(geometry) }

        LaunchedEffect(fall) { fall.run() }

        val paperPainter = painterResource(R.drawable.archive_paper)
        cards.zip(fall.papers).forEach { (card, paper) ->
            Image(
                painter = paperPainter,
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
                        translationX = pileOffsetX + paper.x - paperSizePx / 2f
                        translationY = paper.y - paperSizePx / 2f
                        rotationZ = paper.rotationDegrees
                    }
                    .clickable(role = Role.Button) { onPaperClick(card) },
            )
        }
    }
}

/**
 * 방금 버린 카드가 목록에서 몇 번째인지. 월별 응답에 카드 식별자가 없어 날짜와 그날 순번으로 찾고,
 * 같은 날 여러 장이면 순번이 가장 큰 마지막 장이 방금 버린 것이다.
 *
 * 못 찾으면 null 이라 전부 쏟는 원래 연출로 돌아간다. 달을 바꿔 그 날짜가 목록에서 사라졌을 때가
 * 그렇다.
 */
private fun List<CardEntry>.droppedIndex(date: LocalDate?): Int? {
    if (date == null) return null
    return withIndex()
        .filter { (_, entry) -> entry.date == date }
        .maxByOrNull { (_, entry) -> entry.indexInDate }
        ?.index
}
