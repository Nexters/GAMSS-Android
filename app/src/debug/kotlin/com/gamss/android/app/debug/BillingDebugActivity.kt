package com.gamss.android.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamss.android.core.billing.BillingPurchaseOutcome
import com.gamss.android.core.billing.BillingProduct
import com.gamss.android.core.billing.BillingProductQueryResult
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.designsystem.theme.GamssTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 디버그/검증 전용 화면. 프로덕션 아님.
 * Google 정적 테스트 상품(android.test.*)으로 BillingClient 연결·미처리 구매 복구 조회를
 * 수동으로 검증한다. 현재 Google 공식 문서는 정적 테스트 상품 ID 방식을 더 이상 다루지 않고
 * License Tester로 실제 등록한 상품을 쓰는 방식만 안내하고 있어서, android.test.* 상품은
 * queryProductDetailsAsync로 조회되지 않아 상품 조회 결과·구매창 실행·구매 콜백·consume까지는
 * 확인하지 못했다. 서버 검증·지급 API가 아직 없어 이 화면은 Play 쪽 흐름만 확인한다 — 지급은
 * 다루지 않는다.
 * 실행: adb shell am start -n com.gamss.android.dev/com.gamss.android.app.debug.BillingDebugActivity
 */
@AndroidEntryPoint
class BillingDebugActivity : ComponentActivity() {

    @Inject
    lateinit var billingRepository: BillingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GamssTheme {
                BillingDebugScreen(billingRepository = billingRepository, activity = this)
            }
        }
    }
}

/** 과거 Billing API에서 Play Console 설정 없이 쓸 수 있던 구글 예약 테스트 상품 ID. 현재는
 * 공식 문서가 License Tester + 실제 등록 상품 방식만 안내해서, queryProductDetailsAsync로
 * 조회되지 않아 구매창 실행까지는 확인하지 못했다. */
private val STATIC_TEST_PRODUCT_IDS = listOf(
    "android.test.purchased",
    "android.test.canceled",
    "android.test.item_unavailable",
    "android.test.item_already_owned",
)

@Composable
private fun BillingDebugScreen(billingRepository: BillingRepository, activity: ComponentActivity) {
    val connectionState by billingRepository.connectionState.collectAsState()
    var queryResult by remember { mutableStateOf<BillingProductQueryResult?>(null) }
    var purchaseLog by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastPurchaseToken by remember { mutableStateOf<String?>(null) }
    val obfuscatedAccountId = remember { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        billingRepository.purchaseResults.collect { outcome ->
            purchaseLog = purchaseLog + outcome.toLogLine()
            if (outcome is BillingPurchaseOutcome.Purchased) {
                lastPurchaseToken = outcome.purchaseToken
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Billing 디버그 (정적 테스트 상품)", style = MaterialTheme.typography.titleLarge)
            Text("연결 상태: $connectionState")
            Text("obfuscatedAccountId(테스트용 임의 UUID): $obfuscatedAccountId")

            Button(onClick = { billingRepository.connect() }) {
                Text("연결")
            }

            Button(onClick = { scope.launch { queryResult = billingRepository.queryProductDetails(STATIC_TEST_PRODUCT_IDS) } }) {
                Text("상품 조회")
            }

            Button(onClick = { scope.launch { billingRepository.queryUnfinishedPurchases() } }) {
                Text("복구 조회 (queryUnfinishedPurchases)")
            }

            queryResult?.let { result ->
                Text(
                    "조회 결과: responseCode=${result.callResult.responseCode}, " +
                        "unfetched=${result.unfetchedProductIds}",
                )
                result.products.forEach { product ->
                    ProductRow(
                        product = product,
                        onPurchaseClick = {
                            val launchResult = billingRepository.launchPurchaseFlow(
                                activity = activity,
                                product = product,
                                obfuscatedAccountId = obfuscatedAccountId,
                            )
                            purchaseLog = purchaseLog +
                                "launchBillingFlow(${product.productId}) → " +
                                "responseCode=${launchResult.responseCode}"
                        },
                    )
                }
            }

            lastPurchaseToken?.let { token ->
                Button(
                    onClick = {
                        scope.launch {
                            val result = billingRepository.consumePurchase(token)
                            purchaseLog = purchaseLog + "consumePurchase → responseCode=${result.responseCode}"
                            lastPurchaseToken = null
                        }
                    },
                ) {
                    Text("마지막 구매 소비(재구매 테스트용)")
                }
            }

            Text("구매 콜백 / 복구 조회 로그", style = MaterialTheme.typography.titleMedium)
            purchaseLog.forEach { line -> Text("· $line") }
        }
    }
}

@Composable
private fun ProductRow(product: BillingProduct, onPurchaseClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("${product.productId} · ${product.formattedPrice}")
            Button(onClick = onPurchaseClick) {
                Text("구매")
            }
        }
    }
}

private fun BillingPurchaseOutcome.toLogLine(): String = when (this) {
    is BillingPurchaseOutcome.Purchased -> "PURCHASED token=$purchaseToken products=$productIds"
    is BillingPurchaseOutcome.Pending -> "PENDING products=$productIds"
    is BillingPurchaseOutcome.UserCanceled -> "USER_CANCELED"
    is BillingPurchaseOutcome.AlreadyOwned -> "ITEM_ALREADY_OWNED → 복구 조회 재요청"
    is BillingPurchaseOutcome.Error -> "ERROR code=$responseCode message=$message"
}
