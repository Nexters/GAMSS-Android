package com.gamss.android.feature.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssLogo
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun StoreScreen(
    onNavigateToSetting: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is StoreSideEffect.NavigateToSetting -> onNavigateToSetting()
        }
    }

    StoreContent(
        state = state,
        onSettingClick = viewModel::navigateToSetting,
        modifier = modifier,
    )
}

@Composable
private fun StoreContent(
    state: StoreState,
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        GamssTopBar(
            contentPadding = PaddingValues(start = HeaderStartPadding, end = HeaderEndPadding),
            leading = { GamssLogo(contentDescription = stringResource(R.string.store_logo_description)) },
            trailing = {
                GamssIconButton(
                    iconRes = GamssIcons.Setting,
                    contentDescription = stringResource(R.string.store_setting_description),
                    onClick = onSettingClick,
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardsHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(CardsGap),
        ) {
            state.tokenProducts.forEachIndexed { index, product ->
                StoreTokenCard(
                    product = product,
                    pinRotation = PinRotations[index % PinRotations.size],
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 좌표·각도는 Figma 노드(5773:9086/9089/9091) 기준. 그림자 카드가 뒤에서 살짝 삐져나오는 모양을
 * 흰 카드보다 [ShadowPeekHeight]만큼 키워서 표현한다.
 */
@Composable
private fun StoreTokenCard(
    product: StoreTokenProduct,
    pinRotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(top = PinOverlapHeight)
                .fillMaxWidth()
                .height(CardHeight + ShadowPeekHeight)
                .background(GamssTheme.colors.gray950.copy(alpha = ShadowAlpha))
                .border(CardBorderWidth, GamssTheme.colors.gray950.copy(alpha = ShadowAlpha)),
        )

        Box(
            modifier = Modifier
                .padding(top = PinOverlapHeight)
                .fillMaxWidth()
                .height(CardHeight)
                .background(GamssTheme.colors.white)
                .border(CardBorderWidth, GamssTheme.colors.gray950),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(product.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(product.iconSize),
                )
                Spacer(modifier = Modifier.height(CardLabelGap))
                Text(
                    text = stringResource(product.labelRes),
                    style = GamssTheme.typography.body4Medium,
                    color = GamssTheme.colors.gray950,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = PinWidth, height = PinHeight)
                .rotate(pinRotation)
                .background(GamssTheme.colors.gray950),
        )
    }
}

private val HeaderStartPadding = 20.dp
private val HeaderEndPadding = 8.dp
private val CardsHorizontalPadding = 24.dp
private val CardsGap = 12.dp
private val CardHeight = 120.dp
private val ShadowPeekHeight = 5.dp
private const val ShadowAlpha = 0.2f
private val CardBorderWidth = 1.5.dp
private val PinOverlapHeight = 8.dp
private val PinWidth = 16.dp
private val PinHeight = 7.dp
private val CardLabelGap = 8.dp
// 둘째·셋째가 같은 방향으로 누우면 뻣뻣해 보여서 서로 반대로 기울여 자연스러운 배치처럼 보이게 한다.
private val PinRotations = listOf(45f, -75f, 75f)

@Preview(name = "Store - Light", showBackground = true, widthDp = 402, heightDp = 300)
@Composable
@Suppress("UnusedPrivateMember")
private fun StoreContentLightPreview() {
    GamssTheme(darkTheme = false) {
        StoreContent(state = StoreState(), onSettingClick = {})
    }
}

@Preview(name = "Store - Dark", showBackground = true, widthDp = 402, heightDp = 300)
@Composable
@Suppress("UnusedPrivateMember")
private fun StoreContentDarkPreview() {
    GamssTheme(darkTheme = true) {
        StoreContent(state = StoreState(), onSettingClick = {})
    }
}
