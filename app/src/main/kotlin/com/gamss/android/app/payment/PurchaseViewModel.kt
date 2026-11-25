package com.gamss.android.app.payment

import androidx.lifecycle.ViewModel
import com.gamss.android.app.BuildConfig
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.payment.ConnectBillingUseCase
import com.gamss.android.domain.payment.GetBillingProductsUseCase
import com.gamss.android.domain.payment.GetPaymentAccountTokenUseCase
import com.gamss.android.domain.payment.GetPaymentProductsUseCase
import com.gamss.android.domain.payment.ObserveBillingConnectionUseCase
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProduct
import com.gamss.android.domain.payment.TokenProduct
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import javax.inject.Qualifier

/** Play 라이선스 테스트가 가능한, Play Console에 등록된 패키지(com.gamss.android)로 설치됐는지. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LicensedPackage

@Module
@InstallIn(SingletonComponent::class)
object PurchaseConfigModule {
    @Provides
    @LicensedPackage
    fun provideIsLicensedPackage(): Boolean = BuildConfig.APPLICATION_ID == "com.gamss.android"
}

// 개별 UseCase의 생성자 주입을 유지하기 위해 이 생성자에만 인자 수 제한을 제외한다.
@HiltViewModel
class PurchaseViewModel
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
) : ViewModel(), ContainerHost<PurchaseState, PurchaseSideEffect> {

    override val container = container<PurchaseState, PurchaseSideEffect>(PurchaseState())
    val connectionState = observeBillingConnection()
    private val paymentState = paymentVerifier.state
    private var paymentObserver: Job? = null

    init {
        intent {
            connectionState.collect { if (it == StoreConnectionState.CONNECTED) loadProducts() }
        }
    }

    fun requestRecovery() {
        if (paymentObserver?.isActive != true) {
            paymentObserver = intent {
                paymentState.collect { payment -> reduce { state.copy(payment = payment) } }
            }
        }
        paymentVerifier.requestRecovery()
    }

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

    private suspend fun Syntax<PurchaseState, PurchaseSideEffect>.loadPrices(
        serverProducts: List<TokenProduct>,
    ) {
        if (serverProducts.isEmpty()) {
            reduce { state.copy(isLoading = false, products = emptyList(), error = "판매 중인 상품이 없습니다.") }
            return
        }
        val result = getBillingProducts(serverProducts.map { it.productId })
        val prices = result.products.associateBy { it.productId }
        reduce {
            state.copy(
                isLoading = false,
                products = serverProducts.sortedBy { it.tokenAmount }.map { PurchaseProduct(it, prices[it.productId]) },
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

    private suspend fun Syntax<PurchaseState, PurchaseSideEffect>.preparePurchase(
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
                    postSideEffect(PurchaseSideEffect.LaunchBillingFlow(product, account.data))
                }
            }
        }
    }

    fun onBillingFlowResult(result: BillingOperationResult?) = intent {
        reduce {
            state.copy(
                isPreparing = false,
                error = if (result == null || result.isSuccess) null else "결제창을 열 수 없습니다. (코드: ${result.responseCode})",
            )
        }
    }
}

sealed interface PurchaseSideEffect {
    data class LaunchBillingFlow(val product: StoreProduct, val accountToken: String) : PurchaseSideEffect
}

data class PurchaseProduct(val product: TokenProduct, val billingProduct: StoreProduct?)

data class PurchaseState(
    val products: List<PurchaseProduct> = emptyList(),
    val isLoading: Boolean = false,
    val isPreparing: Boolean = false,
    val payment: PaymentState = PaymentState(),
    val error: String? = null,
)
