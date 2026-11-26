package com.gamss.android.feature.store

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssLogo
import com.gamss.android.core.designsystem.component.GamssNetworkErrorContent
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProduct
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun StoreScreen(
    onNavigateToSetting: () -> Unit,
    launchBillingFlow: (StoreProduct, String) -> BillingOperationResult?,
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current

    DisposableEffect(viewModel) {
        viewModel.requestRecovery()
        onDispose { viewModel.stopObservingPayment() }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is StoreSideEffect.NavigateToSetting -> onNavigateToSetting()
            is StoreSideEffect.LaunchBillingFlow ->
                viewModel.onBillingFlowResult(launchBillingFlow(sideEffect.product, sideEffect.accountToken))
            is StoreSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    // 결제 결과 안내는 상태로 남아 있다가 하나씩 표시되고, 표시한 뒤에 상태에서 제거된다.
    state.payment.notices.firstOrNull()?.let { notice ->
        val message = stringResource(notice.event.messageRes())
        LaunchedEffect(notice.id) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onPaymentNoticeShown(notice.id)
        }
    }

    val busy = state.isLoading || state.isPreparing || state.payment.isVerifying
    val purchaseEnabled = activity != null && !busy && connection == StoreConnectionState.CONNECTED

    Box(modifier = modifier.fillMaxSize()) {
        StoreContent(
            state = state,
            purchaseEnabled = purchaseEnabled,
            onSettingClick = viewModel::navigateToSetting,
            onPurchaseClick = viewModel::purchase,
            onRetryClick = viewModel::loadProducts,
        )
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun StoreContent(
    state: StoreState,
    purchaseEnabled: Boolean,
    onSettingClick: () -> Unit,
    onPurchaseClick: (String) -> Unit,
    onRetryClick: () -> Unit,
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

        val messageModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            state.loadFailure == StoreLoadFailure.NETWORK ->
                GamssNetworkErrorContent(onRefresh = onRetryClick, modifier = messageModifier)
            state.loadFailure == StoreLoadFailure.UNKNOWN -> StoreMessageContent(
                title = stringResource(R.string.store_load_fail_title),
                description = stringResource(R.string.store_load_fail_description),
                actionLabel = stringResource(R.string.store_load_fail_retry),
                onAction = onRetryClick,
                modifier = messageModifier,
            )
            state.isEmpty -> StoreMessageContent(
                title = stringResource(R.string.store_empty_products),
                modifier = messageModifier,
            )
            else -> StoreTokenCards(
                products = state.tokenProducts,
                purchaseEnabled = purchaseEnabled,
                onPurchaseClick = onPurchaseClick,
            )
        }
    }
}

@Composable
private fun StoreTokenCards(
    products: List<StoreTokenProduct>,
    purchaseEnabled: Boolean,
    onPurchaseClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardsHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(CardsGap),
    ) {
        products.forEachIndexed { index, product ->
            StoreTokenCard(
                product = product,
                pinRotation = PinRotations[index % PinRotations.size],
                enabled = purchaseEnabled && product.billingProduct != null,
                onClick = { onPurchaseClick(product.product.productId) },
                modifier = Modifier.weight(1f),
            )
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
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clickable(enabled = enabled, onClick = onClick)) {
        Box(
            modifier = Modifier
                .padding(top = PinOverlapHeight)
                .fillMaxWidth()
                .height(CardHeight + ShadowPeekHeight)
                .background(GamssTheme.colors.gray950.copy(alpha = SHADOW_ALPHA))
                .border(CardBorderWidth, GamssTheme.colors.gray950.copy(alpha = SHADOW_ALPHA)),
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
                    text = stringResource(R.string.store_token_label, product.labelAmountInTenThousands),
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
private const val SHADOW_ALPHA = 0.2f
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
        StoreContent(
            state = StoreState(),
            purchaseEnabled = true,
            onSettingClick = {},
            onPurchaseClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(name = "Store - Dark", showBackground = true, widthDp = 402, heightDp = 300)
@Composable
@Suppress("UnusedPrivateMember")
private fun StoreContentDarkPreview() {
    GamssTheme(darkTheme = true) {
        StoreContent(
            state = StoreState(),
            purchaseEnabled = true,
            onSettingClick = {},
            onPurchaseClick = {},
            onRetryClick = {},
        )
    }
}
