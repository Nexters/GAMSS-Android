package com.gamss.android.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gamss.android.app.R
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.billing.BillingProduct
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.billing.PaymentState
import com.gamss.android.core.billing.paymentMessage
import com.gamss.android.core.designsystem.component.GamssPaperBackground
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProduct
import com.gamss.android.feature.login.LoginScreen
import com.gamss.android.feature.store.StoreSideEffect
import com.gamss.android.feature.store.StoreTokenProduct
import com.gamss.android.feature.store.StoreViewModel
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.text.NumberFormat
import javax.inject.Inject

/**
 * 디버그/검증 전용 화면. 프로덕션 아님.
 * 라이선스 테스트 패키지(com.gamss.android)로 설치해 실제 서버 검증까지 포함한 결제 흐름을 확인한다.
 * 로직은 프로덕션과 같은 [StoreViewModel]을 그대로 쓰고, 화면만 이 Activity 전용으로 둔다.
 * 실행: adb shell am start -n com.gamss.android/com.gamss.android.app.debug.BillingDebugActivity
 */
@AndroidEntryPoint
class BillingDebugActivity : ComponentActivity() {
    @Inject
    lateinit var auth: Lazy<AuthRepository>

    @Inject
    lateinit var billing: BillingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            if (auth.get().sessionState.value == SessionState.Loading) auth.get().restoreSession()
        }
        setContent {
            val session by auth.get().sessionState.collectAsStateWithLifecycle()
            GamssTheme {
                GamssPaperBackground {
                    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        when (session) {
                            SessionState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            SessionState.Unauthenticated -> LoginScreen(
                                googleWebClientId = stringResource(R.string.default_web_client_id),
                                onLoginSuccess = { _ -> },
                            )
                            SessionState.Authenticated -> BillingDebugScreen(
                                onBackClick = ::finish,
                                launchBillingFlow = { product, account ->
                                    if (isFinishing || isDestroyed || product !is BillingProduct) null
                                    else billing.launchPurchaseFlow(this@BillingDebugActivity, product, account)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingDebugScreen(
    onBackClick: () -> Unit,
    launchBillingFlow: (StoreProduct, String) -> BillingOperationResult?,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val busy = state.isLoading || state.isPreparing || state.payment.isVerifying

    DisposableEffect(viewModel) {
        viewModel.requestRecovery()
        onDispose { viewModel.stopObservingPayment() }
    }

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is StoreSideEffect.LaunchBillingFlow ->
                viewModel.onBillingFlowResult(launchBillingFlow(effect.product, effect.accountToken))
            is StoreSideEffect.NavigateToSetting -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GamssTopBar(
            leading = { TextButton(onClick = onBackClick) { GamssText("뒤로") } },
            trailing = { GamssText("결제 디버그") },
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GamssText("Play 연결: $connection")
            BillingDebugStatus(state.payment)
            state.error?.let { GamssText(it) }
            if (busy) CircularProgressIndicator()
            TextButton(onClick = viewModel::loadProducts, enabled = !busy) { GamssText("상품 새로고침") }
            state.tokenProducts.forEach { item ->
                BillingDebugProductCard(
                    item = item,
                    enabled = !busy && connection == StoreConnectionState.CONNECTED,
                    onPurchase = { viewModel.purchase(item.product.productId) },
                )
            }
        }
    }
}

@Composable
private fun BillingDebugStatus(payment: PaymentState) {
    payment.paidBalance?.let { GamssText("지급 응답 시점의 유료 토큰 잔액: ${it.formatted()} 토큰") }
    payment.message?.let { GamssText(it) }
    payment.billingError?.let { GamssText(it.paymentMessage()) }
}

@Composable
private fun BillingDebugProductCard(item: StoreTokenProduct, enabled: Boolean, onPurchase: () -> Unit) {
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
