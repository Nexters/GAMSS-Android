package com.gamss.android.app.payment

import com.gamss.android.core.billing.BillingConnectionState
import com.gamss.android.core.billing.BillingPurchaseOutcome
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.payment.PaymentStatus
import com.gamss.android.domain.payment.VerifyGooglePurchaseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** 구매 화면을 떠나도 결과를 받는 유일한 purchaseResults 소비자. 소비/승인은 서버에서만 한다. */
@Singleton
class PendingPurchaseVerifier @Inject constructor(
    private val billing: BillingRepository,
    private val verifyGooglePurchase: VerifyGooglePurchaseUseCase,
    private val auth: AuthRepository,
    @PaymentScope private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(PaymentState())
    val state = mutableState.asStateFlow()
    private val attemptedPurchases = mutableSetOf<Pair<String, String>>()

    init {
        scope.launch {
            auth.sessionState.collectLatest { session ->
                attemptedPurchases.clear()
                mutableState.value = PaymentState()
                if (session == SessionState.Authenticated) {
                    billing.purchaseResults.collect { handleOutcome(it) }
                }
            }
        }
    }

    fun onResume() {
        if (auth.sessionState.value != SessionState.Authenticated) return
        // 실패한 구매도 다음 복귀에는 다시 조회한다. 같은 복귀 내 중복 콜백은 재전송하지 않는다.
        attemptedPurchases.clear()
        mutableState.value = state.value.copy(billingError = null)
        if (billing.connectionState.value == BillingConnectionState.CONNECTED) {
            scope.launch { billing.queryUnfinishedPurchases() }
        } else {
            billing.connect() // 연결 완료 시 repository가 미처리 구매를 조회한다.
        }
    }

    private suspend fun handleOutcome(outcome: BillingPurchaseOutcome) {
        when (outcome) {
            is BillingPurchaseOutcome.Purchased -> outcome.productIds.forEach { productId ->
                if (attemptedPurchases.add(outcome.purchaseToken to productId)) {
                    verify(productId, outcome.purchaseToken)
                }
            }
            is BillingPurchaseOutcome.Pending -> showMessage("결제 대기 중입니다. 결제가 완료되면 다시 확인합니다.")
            BillingPurchaseOutcome.UserCanceled -> showMessage("구매를 취소했습니다.")
            BillingPurchaseOutcome.AlreadyOwned -> showMessage("미처리 구매를 확인하고 있습니다.")
            is BillingPurchaseOutcome.Error -> {
                mutableState.value = state.value.copy(message = null, billingError = outcome)
            }
        }
    }

    private suspend fun verify(productId: String, purchaseToken: String) {
        mutableState.value = state.value.copy(
            isVerifying = true,
            message = "구매를 검증하고 있습니다.",
            billingError = null,
        )
        val params = VerifyGooglePurchaseUseCase.Params(productId, purchaseToken)
        when (val result = verifyGooglePurchase(params)) {
            is AppResult.Success -> {
                val grant = result.data
                mutableState.value = if (grant.status == PaymentStatus.GRANTED) {
                    PaymentState(
                        paidBalance = grant.paidBalance,
                        message = "${grant.grantedTokens} 토큰 지급 완료",
                    )
                } else {
                    state.value.copy(isVerifying = false, message = "결제 대기 중입니다. 아직 토큰이 지급되지 않았습니다.")
                }
            }
            is AppResult.Failure -> {
                mutableState.value = state.value.copy(isVerifying = false, message = result.throwable.paymentMessage())
            }
        }
        attemptedPurchases.add(purchaseToken to productId)
    }

    private fun showMessage(message: String) {
        mutableState.value = state.value.copy(message = message, billingError = null)
    }
}

data class PaymentState(
    val isVerifying: Boolean = false,
    val paidBalance: Long? = null,
    val message: String? = null,
    val billingError: BillingPurchaseOutcome.Error? = null,
)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PaymentScope

@Module
@InstallIn(SingletonComponent::class)
object PaymentScopeModule {
    @Provides
    @Singleton
    @PaymentScope
    fun provideScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
