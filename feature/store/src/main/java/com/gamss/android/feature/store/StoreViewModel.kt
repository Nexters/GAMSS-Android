package com.gamss.android.feature.store

import androidx.lifecycle.ViewModel
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.billing.BillingResultStatus
import com.gamss.android.core.billing.LicensedPackage
import com.gamss.android.core.billing.PaymentEvent
import com.gamss.android.core.billing.PendingPurchaseVerifier
import com.gamss.android.core.billing.paymentMessage
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.payment.ConnectBillingUseCase
import com.gamss.android.domain.payment.GetBillingProductsUseCase
import com.gamss.android.domain.payment.GetPaymentAccountTokenUseCase
import com.gamss.android.domain.payment.GetPaymentProductsUseCase
import com.gamss.android.domain.payment.ObserveBillingConnectionUseCase
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.TokenProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

// 개별 UseCase의 생성자 주입을 유지하기 위해 이 생성자에만 인자 수 제한을 제외한다.
@HiltViewModel
class StoreViewModel
@Suppress("LongParameterList")
@Inject
constructor(
    private val getPaymentProducts: GetPaymentProductsUseCase,
    private val getPaymentAccountToken: GetPaymentAccountTokenUseCase,
    observeBillingConnection: ObserveBillingConnectionUseCase,
    private val connectBilling: ConnectBillingUseCase,
    private val getBillingProducts: GetBillingProductsUseCase,
    private val paymentVerifier: PendingPurchaseVerifier,
    @LicensedPackage private val isLicensedPackage: Boolean,
) : ViewModel(), ContainerHost<StoreState, StoreSideEffect> {

    override val container = container<StoreState, StoreSideEffect>(StoreState())
    val connectionState = observeBillingConnection()
    private val paymentState = paymentVerifier.state
    private var paymentObserver: Job? = null

    init {
        intent {
            connectionState.collect { if (it == StoreConnectionState.CONNECTED) loadProducts() }
        }
    }

    fun navigateToSetting() = intent {
        postSideEffect(StoreSideEffect.NavigateToSetting)
    }

    fun requestRecovery() {
        if (paymentObserver?.isActive != true) {
            paymentObserver = intent {
                coroutineScope {
                    launch { paymentState.collect { payment -> reduce { state.copy(payment = payment) } } }
                    launch { paymentVerifier.events.collect { event -> postSideEffect(event.toToast()) } }
                }
            }
        }
        paymentVerifier.requestRecovery()
    }

    private fun PaymentEvent.toToast() = StoreSideEffect.ShowToast(
        when (this) {
            is PaymentEvent.Granted -> R.string.store_purchase_complete_toast
            PaymentEvent.VerificationFailed -> R.string.store_purchase_verify_fail_toast
        },
    )

    fun stopObservingPayment() {
        paymentObserver?.cancel()
        paymentObserver = null
    }

    fun loadProducts() = intent {
        if (state.isLoading || state.isPreparing) return@intent
        if (connectionState.value != StoreConnectionState.CONNECTED) {
            connectBilling()
            return@intent
        }
        reduce { state.copy(isLoading = true, error = null) }
        when (val result = getPaymentProducts()) {
            is AppResult.Success -> loadPrices(result.data)
            is AppResult.Failure -> reduce { state.copy(isLoading = false, error = result.throwable.paymentMessage()) }
        }
    }

    private suspend fun Syntax<StoreState, StoreSideEffect>.loadPrices(
        serverProducts: List<TokenProduct>,
    ) {
        if (serverProducts.isEmpty()) {
            reduce { state.copy(isLoading = false, tokenProducts = emptyList(), error = "판매 중인 상품이 없습니다.") }
            return
        }
        val result = getBillingProducts(serverProducts.map { it.productId })
        val prices = result.products.associateBy { it.productId }
        reduce {
            state.copy(
                isLoading = false,
                tokenProducts = serverProducts.sortedBy { it.tokenAmount }
                    .mapIndexed { index, product ->
                        val visual = TokenProductVisuals.getOrElse(index) { TokenProductVisuals.last() }
                        StoreTokenProduct(
                            product = product,
                            billingProduct = prices[product.productId],
                            labelRes = visual.labelRes,
                            iconRes = visual.iconRes,
                            iconSize = visual.iconSize,
                        )
                    },
                error = if (result.isSuccess) null else "Google Play 상품 조회에 실패했습니다.",
            )
        }
    }

    fun purchase(productId: String) = intent {
        if (!isLicensedPackage) return@intent
        if (state.isPreparing || state.isLoading || paymentState.value.isVerifying) return@intent
        reduce { state.copy(isPreparing = true, error = null) }
        preparePurchase(productId)
    }

    private suspend fun Syntax<StoreState, StoreSideEffect>.preparePurchase(
        productId: String,
    ) {
        when (val account = getPaymentAccountToken()) {
            is AppResult.Failure -> reduce {
                state.copy(
                    isPreparing = false,
                    error = account.throwable.paymentMessage(),
                )
            }
            is AppResult.Success -> {
                // 결제 직전 새 ProductDetails를 조회한다. 화면에 저장한 오래된 참조를 사용하지 않는다.
                val query = getBillingProducts(listOf(productId))
                val product = query.products.firstOrNull { it.productId == productId }
                if (!query.isSuccess || product == null) {
                    reduce { state.copy(isPreparing = false, error = "구매 가능한 상품 정보를 찾지 못했습니다.") }
                } else {
                    postSideEffect(StoreSideEffect.LaunchBillingFlow(product, account.data))
                }
            }
        }
    }

    /** 즉시 반환된 결과다. OK는 결제 완료가 아니라 결제창 실행 성공이며, 실제 결과는 구매 콜백으로 들어온다. */
    fun onBillingFlowResult(result: BillingOperationResult?) = intent {
        when (result?.status) {
            BillingResultStatus.OK, BillingResultStatus.USER_CANCELED ->
                reduce { state.copy(isPreparing = false, error = null) }
            BillingResultStatus.ITEM_ALREADY_OWNED -> {
                reduce { state.copy(isPreparing = false, error = null) }
                // 미소비 구매가 하나뿐이라고 단정하지 않고 복구 조회로 전부 다시 찾는다.
                paymentVerifier.requestRecovery()
            }
            BillingResultStatus.ERROR, null -> {
                val code = result?.let { " (코드: ${it.responseCode})" }.orEmpty()
                reduce { state.copy(isPreparing = false, error = "결제창을 열 수 없습니다.$code") }
                postSideEffect(StoreSideEffect.ShowToast(R.string.store_billing_launch_fail_toast))
            }
        }
    }
}
