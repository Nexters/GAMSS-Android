package com.gamss.android.feature.archive.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gamss.android.core.common.util.KoreanTimeZone
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.archive.R
import java.time.YearMonth
import kotlin.math.abs

/**
 * 휠을 돌리는 동안은 고르는 중이고 `선택하기` 를 눌러야 확정된다.
 *
 * 월은 12개월을 그대로 준다 — 카드가 있는 달만 남기려면 두 휠이 서로를 잘라내야 해서, 카드가 없는
 * 달은 빈 상태로 답하는 쪽을 택했다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YearMonthPickerSheet(
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    latest: YearMonth = YearMonth.now(KoreanTimeZone),
) {
    val years = remember(latest) { ((latest.year - YEAR_RANGE_SIZE + 1)..latest.year).toList() }
    val months = remember { (1..MONTHS_IN_YEAR).toList() }
    var pendingYear by remember(selected) { mutableIntStateOf(selected.year) }
    var pendingMonth by remember(selected) { mutableIntStateOf(selected.monthValue) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GamssTheme.colors.white,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = null,
        // 디자인 여백은 화면 맨 아래까지 재는 값이라, 시트가 인셋을 예약하면 그만큼 더 벌어진다.
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(modifier = Modifier.padding(bottom = sheetBottomPadding())) {
            CloseButton(onClick = onDismiss)
            YearMonthWheels(
                years = years,
                months = months,
                selectedYear = pendingYear,
                selectedMonth = pendingMonth,
                onYearChange = { pendingYear = it },
                onMonthChange = { pendingMonth = it },
            )
            ConfirmButton(onClick = { onSelect(YearMonth.of(pendingYear, pendingMonth)) })
        }
    }
}

/**
 * 제스처 바(24dp)는 디자인의 홈 인디케이터처럼 여백 위에 겹쳐도 된다. 3버튼 내비처럼 인셋이 디자인
 * 여백보다 크면 버튼이 내비바에 붙으므로, 그때는 인셋 위로 최소 간격만큼 띄운다.
 */
@Composable
private fun sheetBottomPadding(): Dp {
    val systemBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return SheetBottomPadding.coerceAtLeast(systemBarInset + MinGapAboveSystemBar)
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // hitPadding 만큼 당겨, 아이콘 자체가 디자인 위치에 오게 한다.
            .padding(top = SheetTopPadding - HitPadding, end = SheetHorizontalPadding - HitPadding),
        horizontalArrangement = Arrangement.End,
    ) {
        GamssIconButton(
            iconRes = GamssIcons.Close,
            contentDescription = stringResource(R.string.archive_month_picker_close),
            onClick = onClick,
            hitPadding = HitPadding,
            tint = GamssTheme.colors.gray400,
        )
    }
}

/** 가운데 칸 하이라이트는 두 휠에 걸친 한 덩어리라, 휠 뒤에 한 번만 깔고 가운데 정렬한다. */
@Composable
private fun YearMonthWheels(
    years: List<Int>,
    months: List<Int>,
    selectedYear: Int,
    selectedMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(
                start = SheetHorizontalPadding,
                end = SheetHorizontalPadding,
                // 위 Row 가 아이콘 아래에도 hitPadding 을 물고 있어 그만큼 뺀다.
                top = WheelTopGap - HitPadding,
            )
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelRowHeight)
                .background(GamssTheme.colors.gray200, RoundedCornerShape(WheelHighlightRadius)),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            PickerWheel(
                items = years,
                selected = selectedYear,
                label = { stringResource(R.string.archive_picker_year, it) },
                onSelect = onYearChange,
                modifier = Modifier.weight(1f),
            )
            PickerWheel(
                items = months,
                selected = selectedMonth,
                label = { stringResource(R.string.archive_picker_month, it) },
                onSelect = onMonthChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConfirmButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(
                start = ConfirmHorizontalPadding,
                end = ConfirmHorizontalPadding,
                top = ConfirmTopPadding,
            )
            .fillMaxWidth()
            .height(ConfirmHeight),
        shape = RoundedCornerShape(ConfirmCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = GamssTheme.colors.gray900,
            contentColor = GamssTheme.colors.white,
        ),
    ) {
        Text(
            text = stringResource(R.string.archive_month_picker_confirm),
            style = GamssTheme.typography.title5,
        )
    }
}

/**
 * 가운데 칸에 놓인 값이 고른 값이 되는 휠. 위아래로 [WHEEL_VISIBLE_ROWS] / 2 칸만큼 여백을 줘서
 * 첫 칸과 마지막 칸도 가운데까지 올라올 수 있게 한다.
 *
 * [selected] 는 처음 어디에 멈춰 있을지만 정한다. 그 뒤로는 휠이 스크롤 위치를 직접 들고 있고,
 * 가운데 값이 바뀔 때마다 [onSelect] 로 알린다.
 */
@Composable
private fun PickerWheel(
    items: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    // 스크롤 도중에도 가운데가 흔들리지 않게, 오프셋을 어림하지 않고 실제로 화면 중앙에 가장 가까운 칸을 찾는다.
    // 첫 프레임에는 아직 레이아웃이 없어 가운데를 알 수 없다. 그때 0 번째로 떨어지면 고르지도 않은 첫 칸이
    // 선택돼 버리므로, 레이아웃이 잡히기 전까지는 지금 고른 칸을 그대로 답한다.
    val centeredIndex by remember(selectedIndex) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                ?.index
                ?: selectedIndex
        }
    }

    LaunchedEffect(items) {
        snapshotFlow { centeredIndex }.collect { index -> items.getOrNull(index)?.let(onSelect) }
    }

    LazyColumn(
        modifier = modifier.height(WheelRowHeight * WHEEL_VISIBLE_ROWS),
        state = listState,
        contentPadding = PaddingValues(vertical = WheelRowHeight * (WHEEL_VISIBLE_ROWS / 2)),
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(items) { index, value ->
            Text(
                text = label(value),
                modifier = Modifier
                    .height(WheelRowHeight)
                    .wrapContentHeight(Alignment.CenterVertically),
                style = GamssTheme.typography.body3Regular,
                color = if (index == centeredIndex) GamssTheme.colors.gray900 else GamssTheme.colors.gray400,
            )
        }
    }
}

private const val YEAR_RANGE_SIZE = 5
private const val MONTHS_IN_YEAR = 12
private const val WHEEL_VISIBLE_ROWS = 5

private val SheetCornerRadius = 20.dp
private val SheetTopPadding = 24.dp
private val SheetBottomPadding = 32.dp
private val MinGapAboveSystemBar = 8.dp
private val SheetHorizontalPadding = 20.dp
private val HitPadding = 12.dp
private val WheelTopGap = 24.dp
private val WheelRowHeight = 40.dp
private val WheelHighlightRadius = 12.dp
private val ConfirmHorizontalPadding = 18.dp
private val ConfirmTopPadding = 24.dp
private val ConfirmHeight = 52.dp
private val ConfirmCornerRadius = 12.dp

@Preview(showBackground = true, widthDp = 402)
@Composable
@Suppress("UnusedPrivateMember")
private fun YearMonthPickerSheetPreview() {
    GamssTheme(darkTheme = false) {
        YearMonthPickerSheet(
            selected = YearMonth.of(2026, 7),
            onSelect = {},
            onDismiss = {},
            latest = YearMonth.of(2026, 8),
        )
    }
}
