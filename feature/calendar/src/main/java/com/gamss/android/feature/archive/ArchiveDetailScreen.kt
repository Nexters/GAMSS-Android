package com.gamss.android.feature.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.component.YearMonthPickerSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.orbitmvi.orbit.compose.collectAsState
import java.time.LocalDate
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

    ArchiveDetailFrame(
        emotion = emotion,
        state = state,
        onBackClick = onBackClick,
        onMonthClick = viewModel::showMonthPicker,
        onMonthSelect = viewModel::selectMonth,
        onMonthPickerDismiss = viewModel::dismissMonthPicker,
    )
}

@Composable
private fun ArchiveDetailFrame(
    emotion: EmotionCharacter,
    state: ArchiveDetailState,
    onBackClick: () -> Unit,
    onMonthClick: () -> Unit,
    onMonthSelect: (YearMonth) -> Unit,
    onMonthPickerDismiss: () -> Unit,
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
        ArchiveDetailContent(
            state = state,
            onMonthClick = onMonthClick,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (state.isMonthPickerVisible) {
        YearMonthPickerSheet(
            selected = state.yearMonth,
            onSelect = onMonthSelect,
            onDismiss = onMonthPickerDismiss,
        )
    }
}

@Composable
private fun ArchiveDetailContent(
    state: ArchiveDetailState,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        MonthSelector(
            yearMonth = state.yearMonth,
            onClick = onMonthClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
private fun MonthSelector(
    yearMonth: YearMonth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                start = DetailHorizontalPadding,
                end = DetailHorizontalPadding,
                top = DetailMonthTopPadding,
            )
            .fillMaxWidth()
            .height(DetailMonthHeight)
            // 입력바와 같은 손그림 테두리 에셋을 그대로 쓴다. 화면마다 따로 만들면 같은 그림이 리소스로
            // 중복된다.
            .paint(
                painter = painterResource(com.gamss.android.core.designsystem.R.drawable.bg_input_box),
                contentScale = ContentScale.FillBounds,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = DetailMonthHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = yearMonth.format(YearMonthFormatter),
            style = GamssTheme.typography.title4,
            color = GamssTheme.colors.gray900,
        )
        Image(
            painter = painterResource(com.gamss.android.core.designsystem.R.drawable.ic_right_chevron),
            contentDescription = stringResource(R.string.archive_select_month_description),
            // 글리프가 뷰포트 오른쪽에 몰려 있어, 박스 중심으로 돌리면 그 가로 편차가 세로 어긋남으로
            // 바뀐다. 회전축을 글리프 중심에 두면 제자리에서 돌아 세로 중앙에 남는다.
            // 대신 돌아간 잉크가 레이아웃 박스를 오른쪽으로 넘어가, 그만큼 밀어 좌우 여백을 맞춘다.
            modifier = Modifier
                .padding(end = DetailChevronInkOverflow)
                .size(DetailChevronSize)
                .graphicsLayer {
                    rotationZ = CHEVRON_ROTATION
                    transformOrigin = TransformOrigin(CHEVRON_CENTER_X, CHEVRON_CENTER_Y)
                },
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
private fun PaperPile(cards: List<CardEntry>) {
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
        val radiusPx = paperSizePx / 2f * PAPER_COLLISION_RADIUS_SCALE

        // 화면 회전 등으로 boundsWidthPx/boundsHeightPx만 바뀌었을 때는 키에서 빼서, 이미 쌓인 카드가
        // 처음부터 다시 쏟아지지 않게 한다 — cards 자체가 바뀔 때만 스폰/낙하를 새로 시작한다.
        val uiStates = remember(cards) {
            List(cards.size) { index ->
                val spawnX = radiusPx + Random.nextFloat() * (boundsWidthPx - radiusPx * 2f).coerceAtLeast(0f)
                val spawnY = -radiusPx - index * radiusPx * PAPER_SPAWN_STAGGER
                val spawnRotation = (Random.nextFloat() - 0.5f) * 2f * PAPER_MAX_TILT_DEGREES
                PaperUiState(x = spawnX, y = spawnY, rotationDegrees = spawnRotation)
            }
        }

        LaunchedEffect(cards) {
            runPaperFall(
                uiStates = uiStates,
                boundsWidthPx = boundsWidthPx,
                boundsHeightPx = boundsHeightPx,
                radiusPx = radiusPx,
            )
        }

        cards.forEachIndexed { index, card ->
            val ui = uiStates.getOrNull(index) ?: return@forEachIndexed
            Image(
                painter = painterResource(R.drawable.archive_paper),
                contentDescription = stringResource(
                    R.string.archive_paper_description,
                    card.date.monthValue,
                    card.date.dayOfMonth,
                ),
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
            startVelX = (Random.nextFloat() - 0.5f) * PAPER_SPAWN_DRIFT,
            startAngularVelocity = (Random.nextFloat() - 0.5f) * PAPER_SPAWN_SPIN,
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
                PHYSICS_FIXED_DT
            } else {
                ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, PHYSICS_MAX_DT)
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
            settledFrames = if (world.maxActivity() < PHYSICS_SETTLE_THRESHOLD) settledFrames + 1 else 0
        }
        val elapsedNanos = lastFrameNanos - startFrameNanos
        if (settledFrames >= PHYSICS_SETTLE_FRAMES || elapsedNanos > PHYSICS_MAX_DURATION_NANOS) break
    }
}

private const val CHEVRON_ROTATION = 90f

// ic_right_chevron 글리프의 실제 중심. 24 뷰포트에서 stroke 포함 x 12.4~21.3, y 3.9~20.2 다.
private const val CHEVRON_CENTER_X = 16.85f / 24f
private const val CHEVRON_CENTER_Y = 12.03f / 24f

private const val PAPER_COLLISION_RADIUS_SCALE = 1.15f
private const val PAPER_MAX_TILT_DEGREES = 42f
private const val PAPER_SPAWN_STAGGER = 0.9f
private const val PAPER_SPAWN_DRIFT = 120f
private const val PAPER_SPAWN_SPIN = 0.15f
private const val PHYSICS_FIXED_DT = 1f / 60f
private const val PHYSICS_MAX_DT = 1f / 30f
private const val PHYSICS_SETTLE_THRESHOLD = 4f
private const val PHYSICS_SETTLE_FRAMES = 30
private const val PHYSICS_MAX_DURATION_NANOS = 5_000_000_000L

private val YearMonthFormatter = DateTimeFormatter.ofPattern("yyyy.MM")
private val DetailTopBarStartPadding = 18.dp
private val DetailTopBarEndPadding = 18.dp
private val DetailBackHitPadding = 0.dp
private val DetailTitleStartPadding = 12.dp
private val DetailHorizontalPadding = 18.dp
private val DetailMonthTopPadding = 24.dp
private val DetailMonthHeight = 48.dp
private val DetailMonthHorizontalPadding = 12.dp

// 디자인의 셰브론 프레임은 18dp 다. 드로어블 고유 크기(24dp)로 두면 그만큼 커 보인다.
private val DetailChevronSize = 18.dp

// 회전한 잉크가 레이아웃 박스를 넘는 양(뷰포트 기준 1dp × 18/24)에 텍스트 사이드베어링을 더한 값.
private val DetailChevronInkOverflow = 1.dp
private val DetailPaperPileTopPadding = 136.dp
private val DetailPaperDesignWidth = 402.dp
private val DetailPaperSize = 88.dp

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
@Suppress("UnusedPrivateMember")
private fun ArchiveDetailPaperPilePreview() {
    GamssTheme(darkTheme = false) {
        ArchiveDetailFrame(
            emotion = EmotionCharacter.QUIRKY,
            state = ArchiveDetailState(
                emotion = EmotionCharacter.QUIRKY,
                yearMonth = YearMonth.of(2026, 7),
                isLoading = false,
                cards = List(24) { index -> PreviewCard.copy(indexInDate = index) },
            ),
            onBackClick = {},
            onMonthClick = {},
            onMonthSelect = {},
            onMonthPickerDismiss = {},
        )
    }
}

private val PreviewCard = CardEntry(
    date = LocalDate.of(2026, 7, 23),
    indexInDate = 0,
    character = EmotionCharacter.QUIRKY,
)
