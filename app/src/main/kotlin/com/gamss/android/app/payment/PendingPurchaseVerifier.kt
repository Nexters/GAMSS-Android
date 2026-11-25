package com.gamss.android.app.payment

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.gamss.android.core.billing.BillingConnectionState
import com.gamss.android.core.billing.BillingPurchaseOutcome
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.payment.PaymentStatus
import com.gamss.android.domain.payment.VerifyGooglePurchaseUseCase
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** 앱 프로세스에서 구매 결과를 수신하고 서버 검증과 복구를 담당한다. */
interface PendingPurchaseVerifier {
    val state: StateFlow<PaymentState>

    /** Application에서 한 번 호출한다. 최초 onStart와 이후 포그라운드 복귀를 처리한다. */
    fun start()

    fun requestRecovery()
}

@Singleton
internal class PendingPurchaseVerifierImpl @Inject constructor(
    private val billing: BillingRepository,
    private val verifyGooglePurchase: VerifyGooglePurchaseUseCase,
    private val auth: AuthRepository,
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier,
    @PaymentScope private val scope: CoroutineScope,
) : PendingPurchaseVerifier {
    private val mutableState = MutableStateFlow(PaymentState())
    override val state = mutableState.asStateFlow()
    private val attemptedPurchases = mutableSetOf<Pair<String, String>>()
    private var recoveryInProgress = false

    init {
        scope.launch {
            auth.sessionState.collectLatest { session ->
                attemptedPurchases.clear()
                mutableState.value = PaymentState()
                if (session == SessionState.Authenticated) {
                    requestRecovery()
                    billing.purchaseResults.collect { handleOutcome(it) }
                }
            }
        }
    }

    override fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = requestRecovery()
        })
    }

    override fun requestRecovery() {
        if (auth.sessionState.value != SessionState.Authenticated) return
        if (recoveryInProgress) return
        // 실패한 구매도 다음 복귀에는 다시 조회한다. 같은 복귀 내 중복 콜백은 재전송하지 않는다.
        attemptedPurchases.clear()
        mutableState.value = state.value.copy(billingError = null)
        if (billing.connectionState.value == BillingConnectionState.CONNECTED) {
            recoveryInProgress = true
            scope.launch {
                try {
                    billing.queryUnfinishedPurchases()
                } finally {
                    recoveryInProgress = false
                }
            }
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
            BillingPurchaseOutcome.UserCanceled -> {
                mutableState.value = state.value.copy(message = null, billingError = null)
            }
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
                    tokenUsageRefreshNotifier.requestRefresh()
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
internal abstract class PendingPurchaseVerifierModule {
    @Binds
    abstract fun bindPendingPurchaseVerifier(impl: PendingPurchaseVerifierImpl): PendingPurchaseVerifier

    companion object {
        @Provides
        @Singleton
        @PaymentScope
        fun provideScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
