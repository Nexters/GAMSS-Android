package com.gamss.android.app.payment

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.gamss.android.app.BuildConfig
import com.gamss.android.core.billing.BillingConnectionState
import com.gamss.android.core.billing.BillingProduct
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.payment.GetPaymentAccountTokenUseCase
import com.gamss.android.domain.payment.GetPaymentProductsUseCase
import com.gamss.android.domain.payment.TokenProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val getPaymentProducts: GetPaymentProductsUseCase,
    private val getPaymentAccountToken: GetPaymentAccountTokenUseCase,
    private val billing: BillingRepository,
    val verifier: PendingPurchaseVerifier,
) : ViewModel(), ContainerHost<PurchaseState, Unit> {

    override val container = container<PurchaseState, Unit>(PurchaseState())
    val connectionState = billing.connectionState

    init {
        intent {
            connectionState.collect { if (it == BillingConnectionState.CONNECTED) loadProducts() }
        }
    }

    fun loadProducts() = intent {
        if (state.isLoading || state.isPreparing) return@intent
        if (connectionState.value != BillingConnectionState.CONNECTED) {
            billing.connect()
            return@intent
        }
        reduce { state.copy(isLoading = true, error = null) }
        when (val result = getPaymentProducts()) {
            is AppResult.Success -> loadPrices(result.data)
            is AppResult.Failure -> reduce { state.copy(isLoading = false, error = result.throwable.paymentMessage()) }
        }
    }

    private suspend fun Syntax<PurchaseState, Unit>.loadPrices(
        serverProducts: List<TokenProduct>,
    ) {
        // TEMP: 서버 상품 설정이 Play Console ID로 수정되면 이 보정을 제거한다.
        val products = serverProducts.map {
            it.copy(productId = PLAY_TEST_PRODUCT_IDS[it.productId] ?: it.productId)
        }
        if (products.isEmpty()) {
            reduce { state.copy(isLoading = false, products = emptyList(), error = "판매 중인 상품이 없습니다.") }
            return
        }
        val result = billing.queryProductDetails(products.map { it.productId })
        val prices = result.products.associateBy { it.productId }
        reduce {
            state.copy(
                isLoading = false,
                products = products.sortedBy { it.tokenAmount }.map { PurchaseProduct(it, prices[it.productId]) },
                error = if (result.callResult.isSuccess) null else "Google Play 상품 조회에 실패했습니다.",
            )
        }
    }

    fun purchase(activity: Activity, productId: String) = intent {
        if (BuildConfig.APPLICATION_ID != "com.gamss.android") return@intent
        if (state.isPreparing || state.isLoading || verifier.state.value.isVerifying) return@intent
        reduce { state.copy(isPreparing = true, error = null) }
        preparePurchase(activity, productId)
    }

    private suspend fun Syntax<PurchaseState, Unit>.preparePurchase(
        activity: Activity,
        productId: String,
    ) {
        when (val account = getPaymentAccountToken()) {
            is AppResult.Failure -> reduce { state.copy(isPreparing = false, error = account.throwable.paymentMessage()) }
            is AppResult.Success -> {
                // 결제 직전 새 ProductDetails를 조회한다. 화면에 저장한 오래된 참조를 사용하지 않는다.
                val query = billing.queryProductDetails(listOf(productId))
                val product = query.products.firstOrNull { it.productId == productId }
                if (!query.callResult.isSuccess || product == null) {
                    reduce { state.copy(isPreparing = false, error = "구매 가능한 상품 정보를 찾지 못했습니다.") }
                } else if (!activity.isFinishing && !activity.isDestroyed) {
                    val result = billing.launchPurchaseFlow(activity, product, account.data)
                    reduce {
                        state.copy(
                            isPreparing = false,
                            error = if (result.isSuccess) null else "결제창을 열 수 없습니다. (코드: ${result.responseCode})",
                        )
                    }
                } else {
                    reduce { state.copy(isPreparing = false) }
                }
            }
        }
    }
}

data class PurchaseProduct(val product: TokenProduct, val billingProduct: BillingProduct?)

data class PurchaseState(
    val products: List<PurchaseProduct> = emptyList(),
    val isLoading: Boolean = false,
    val isPreparing: Boolean = false,
    val error: String? = null,
)

private val PLAY_TEST_PRODUCT_IDS = mapOf(
    "token_300000" to "token_300k",
    "token_500000" to "token_500k",
    "token_1000000" to "token_1m",
)
