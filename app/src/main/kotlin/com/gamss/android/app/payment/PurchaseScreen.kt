package com.gamss.android.app.payment

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamss.android.app.BuildConfig
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProduct
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.text.NumberFormat

@Composable
fun PurchaseScreen(
    onBackClick: () -> Unit,
    launchBillingFlow: (StoreProduct, String) -> BillingOperationResult?,
    viewModel: PurchaseViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val canPurchase = BuildConfig.APPLICATION_ID == "com.gamss.android"
    val busy = state.isLoading || state.isPreparing || state.payment.isVerifying

    DisposableEffect(viewModel) {
        viewModel.requestRecovery()
        onDispose { viewModel.stopObservingPayment() }
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is PurchaseSideEffect.LaunchBillingFlow ->
                viewModel.onBillingFlowResult(launchBillingFlow(effect.product, effect.accountToken))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GamssTopBar(
            leading = { TextButton(onClick = onBackClick) { GamssText("뒤로") } },
            trailing = { GamssText("토큰 구매") },
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GamssText("Google Play 토큰 구매")
            GamssText("가격과 결제 수단은 Google Play에서 확인해 주세요.")
            if (!canPurchase) GamssText("라이선스 테스트는 Play에 등록된 패키지(com.gamss.android)에서 진행해 주세요.")
            GamssText("Play 연결: $connection")
            PaymentStatusContent(state.payment)
            state.error?.let { GamssText(it) }
            if (busy) CircularProgressIndicator()
            TextButton(onClick = viewModel::loadProducts, enabled = !busy) { GamssText("상품 새로고침") }
            state.products.forEach { item ->
                PurchaseProductCard(
                    item = item,
                    enabled = canPurchase && !busy && activity != null &&
                        connection == StoreConnectionState.CONNECTED,
                    onPurchase = { viewModel.purchase(item.product.productId) },
                )
            }
            GamssText("결제 대기 중에는 토큰이 지급되지 않습니다. 미처리 구매는 앱에 다시 돌아올 때 자동으로 확인합니다.")
        }
    }
}

@Composable
private fun PaymentStatusContent(payment: PaymentState) {
    payment.paidBalance?.let { GamssText("지급 응답 시점의 유료 토큰 잔액: ${it.formatted()} 토큰") }
    payment.message?.let { GamssText(it) }
    payment.billingError?.let { GamssText(it.paymentMessage()) }
}

@Composable
private fun PurchaseProductCard(item: PurchaseProduct, enabled: Boolean, onPurchase: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GamssText("${item.product.tokenAmount.formatted()} 토큰")
            GamssText(item.product.productId)
            GamssText(item.billingProduct?.formattedPrice ?: "Google Play에서 상품을 찾을 수 없습니다.")
            Button(onClick = onPurchase, enabled = enabled && item.billingProduct != null) {
                GamssText("구매", color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

private fun Long.formatted(): String = NumberFormat.getIntegerInstance().format(this)
