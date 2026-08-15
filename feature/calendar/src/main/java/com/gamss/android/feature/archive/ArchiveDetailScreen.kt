package com.gamss.android.feature.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.orbitmvi.orbit.compose.collectAsState
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun ArchiveDetailScreen(
    emotion: EmotionCharacter,
    onBackClick: () -> Unit,
    viewModel: ArchiveDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    LaunchedEffect(emotion) { viewModel.load(emotion) }
    BackHandler(onBack = onBackClick)

    ArchiveDetailFrame(emotion = emotion, state = state, onBackClick = onBackClick)
}

@Composable
private fun ArchiveDetailFrame(
    emotion: EmotionCharacter,
    state: ArchiveDetailState,
    onBackClick: () -> Unit,
) {
    Scaffold(
        containerColor = GamssTheme.colors.white,
        topBar = {
            GamssTopBar(
                contentPadding = PaddingValues(start = DetailTopBarStartPadding, end = DetailTopBarEndPadding),
                leading = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GamssIconButton(
                            iconRes = com.gamss.android.core.designsystem.R.drawable.ic_left_chevron,
                            contentDescription = stringResource(R.string.archive_back_description),
                            onClick = onBackClick,
                            hitPadding = DetailBackHitPadding,
                        )
                        Text(
                            text = emotion.displayName,
                            style = GamssTheme.typography.title4,
                            color = GamssTheme.colors.gray900,
                            modifier = Modifier.padding(start = DetailTitleStartPadding),
                        )
                    }
                },
                trailing = {
                    // 비우기는 실제 카드/대화를 삭제하는 파괴적 동작이므로 확인 UI가 준비되기 전에는 표시만 한다.
                    Text(
                        text = stringResource(R.string.archive_clear),
                        style = GamssTheme.typography.body4Medium,
                        color = GamssTheme.colors.gray900,
                    )
                },
            )
        },
    ) { innerPadding ->
        ArchiveDetailContent(state = state, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun ArchiveDetailContent(
    state: ArchiveDetailState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        MonthSelector(modifier = Modifier.align(Alignment.TopCenter))
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GamssTheme.colors.gray700)
            }

            state.loadFailed -> Text(
                text = stringResource(R.string.archive_cards_load_failed),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray500,
                modifier = Modifier.align(Alignment.Center),
            )

            state.cards.isEmpty() -> Text(
                text = stringResource(R.string.archive_cards_empty),
                style = GamssTheme.typography.body4Medium,
                color = GamssTheme.colors.gray500,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> PaperPile(cards = state.cards)
        }
    }
}

@Composable
private fun MonthSelector(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(
                start = DetailHorizontalPadding,
                end = DetailHorizontalPadding,
                top = DetailMonthTopPadding,
            )
            .fillMaxWidth()
            .height(DetailMonthHeight)
            .border(DetailMonthBorderWidth, GamssTheme.colors.gray900)
            .padding(horizontal = DetailMonthHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = YearMonth.now(KoreanTimeZone).format(YearMonthFormatter),
            style = GamssTheme.typography.title4,
            color = GamssTheme.colors.gray900,
        )
        Image(
            painter = painterResource(com.gamss.android.core.designsystem.R.drawable.ic_right_chevron),
            contentDescription = null,
            modifier = Modifier.graphicsLayer { rotationZ = 90f },
        )
    }
}

@Stable
private class PaperUiState(x: Float, y: Float, rotationDegrees: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var rotationDegrees by mutableFloatStateOf(rotationDegrees)
}

@Composable
private fun PaperPile(cards: List<Card>) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = DetailPaperPileTopPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val scale = (maxWidth / DetailPaperDesignWidth).coerceAtMost(1f)
        val density = LocalDensity.current
        val boundsWidthPx = with(density) { (DetailPaperDesignWidth * scale).toPx() }
        val boundsHeightPx = with(density) { maxHeight.toPx() }
        val paperSizePx = with(density) { (DetailPaperSize * scale).toPx() }
        val radiusPx = paperSizePx / 2f * PaperCollisionRadiusScale

        // 화면 회전 등으로 boundsWidthPx/boundsHeightPx만 바뀌었을 때는 키에서 빼서, 이미 쌓인 카드가
        // 처음부터 다시 쏟아지지 않게 한다 — cards 자체가 바뀔 때만 스폰/낙하를 새로 시작한다.
        val uiStates = remember(cards) {
            List(cards.size) { index ->
                val spawnX = radiusPx + Random.nextFloat() * (boundsWidthPx - radiusPx * 2f).coerceAtLeast(0f)
                val spawnY = -radiusPx - index * radiusPx * PaperSpawnStagger
                val spawnRotation = (Random.nextFloat() - 0.5f) * 2f * PaperMaxTiltDegrees
                PaperUiState(x = spawnX, y = spawnY, rotationDegrees = spawnRotation)
            }
        }

        LaunchedEffect(cards) {
            runPaperFall(uiStates = uiStates, boundsWidthPx = boundsWidthPx, boundsHeightPx = boundsHeightPx, radiusPx = radiusPx)
        }

        cards.forEachIndexed { index, card ->
            val ui = uiStates.getOrNull(index) ?: return@forEachIndexed
            Image(
                painter = painterResource(R.drawable.archive_paper),
                contentDescription = card.summary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(DetailPaperSize * scale)
                    .graphicsLayer {
                        translationX = ui.x - paperSizePx / 2f
                        translationY = ui.y - paperSizePx / 2f
                        rotationZ = ui.rotationDegrees
                    },
            )
        }
    }
}

/** [PaperPile]의 낙하 물리 루프. 매 프레임 [PaperPhysicsWorld]를 갱신해 [uiStates]에 반영하고, 다 쌓이면 멈춘다. */
private suspend fun CoroutineScope.runPaperFall(
    uiStates: List<PaperUiState>,
    boundsWidthPx: Float,
    boundsHeightPx: Float,
    radiusPx: Float,
) {
    val bodies = uiStates.map { ui ->
        PaperBody(
            startX = ui.x,
            startY = ui.y,
            startAngle = ui.rotationDegrees.toRadians(),
            radius = radiusPx,
            startVelX = (Random.nextFloat() - 0.5f) * PaperSpawnDrift,
            startAngularVelocity = (Random.nextFloat() - 0.5f) * PaperSpawnSpin,
        )
    }
    val world = PaperPhysicsWorld(bodies, boundsWidth = boundsWidthPx, boundsHeight = boundsHeightPx)

    var lastFrameNanos = -1L
    var startFrameNanos = -1L
    var settledFrames = 0
    while (isActive) {
        withFrameNanos { frameNanos ->
            if (startFrameNanos < 0) startFrameNanos = frameNanos
            val dt = if (lastFrameNanos < 0) {
                PhysicsFixedDt
            } else {
                ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, PhysicsMaxDt)
            }
            lastFrameNanos = frameNanos

            world.step(dt)
            bodies.forEachIndexed { index, body ->
                uiStates[index].apply {
                    x = body.x
                    y = body.y
                    rotationDegrees = body.angle.toDegrees()
                }
            }
            settledFrames = if (world.maxActivity() < PhysicsSettleThreshold) settledFrames + 1 else 0
        }
        val elapsedNanos = lastFrameNanos - startFrameNanos
        if (settledFrames >= PhysicsSettleFrames || elapsedNanos > PhysicsMaxDurationNanos) break
    }
}

private const val PaperCollisionRadiusScale = 1.15f
private const val PaperMaxTiltDegrees = 42f
private const val PaperSpawnStagger = 0.9f
private const val PaperSpawnDrift = 120f
private const val PaperSpawnSpin = 0.15f
private const val PhysicsFixedDt = 1f / 60f
private const val PhysicsMaxDt = 1f / 30f
private const val PhysicsSettleThreshold = 4f
private const val PhysicsSettleFrames = 30
private const val PhysicsMaxDurationNanos = 5_000_000_000L

private val YearMonthFormatter = DateTimeFormatter.ofPattern("yyyy.MM")
private val DetailTopBarStartPadding = 18.dp
private val DetailTopBarEndPadding = 18.dp
private val DetailBackHitPadding = 0.dp
private val DetailTitleStartPadding = 12.dp
private val DetailHorizontalPadding = 18.dp
private val DetailMonthTopPadding = 24.dp
private val DetailMonthHeight = 48.dp
private val DetailMonthBorderWidth = 1.dp
private val DetailMonthHorizontalPadding = 12.dp
private val DetailPaperPileTopPadding = 136.dp
private val DetailPaperDesignWidth = 402.dp
private val DetailPaperSize = 88.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ArchiveDetailPaperPilePreview() {
    GamssTheme(darkTheme = false) {
        ArchiveDetailFrame(
            emotion = EmotionCharacter.QUIRKY,
            state = ArchiveDetailState(
                emotion = EmotionCharacter.QUIRKY,
                isLoading = false,
                cards = List(24) { PreviewCard },
            ),
            onBackClick = {},
        )
    }
}

private val PreviewCard = Card(
    character = EmotionCharacter.QUIRKY,
    summary = "미리보기 카드",
    message = "",
)
