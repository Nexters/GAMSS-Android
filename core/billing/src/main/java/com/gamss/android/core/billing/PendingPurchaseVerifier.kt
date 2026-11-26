package com.gamss.android.core.billing

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 앱 프로세스에서 구매 결과를 수신하고 서버 검증과 복구를 담당한다. 모든 메서드는 어느 스레드에서 호출해도
 * 된다 — 내부 상태 변경은 항상 [PaymentScope](메인 스레드) 안에서만 일어난다.
 */
interface PendingPurchaseVerifier {
    val state: StateFlow<PaymentState>

    /** Application에서 한 번 호출한다. 최초 onStart와 이후 포그라운드 복귀를 처리한다. */
    fun start()

    fun requestRecovery()

    /** UI가 [PaymentState.notices]의 안내를 표시한 뒤 호출한다. 호출 전까지 안내는 상태에 남는다. */
    fun onNoticeShown(noticeId: Long)
}

@Singleton
internal class PendingPurchaseVerifierImpl @Inject constructor(
    private val billing: BillingRepository,
    private val verifyGooglePurchase: VerifyGooglePurchaseUseCase,
    private val auth: AuthRepository,
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier,
    @PaymentScope private val scope: CoroutineScope,
) : PendingPurchaseVerifier {
    // 아래 상태는 모두 [scope](Main) 안에서만 읽고 쓴다. 외부 진입점은 scope로 넘겨 처리해 스레드 경합을 막는다.
    // (Orbit intent 등 Default 스레드에서도 호출된다.)
    private val mutableState = MutableStateFlow(PaymentState())
    override val state = mutableState.asStateFlow()
    private var nextNoticeId = 0L
    private val attemptedPurchases = mutableSetOf<Pair<String, String>>()

    // 같은 구매의 같은 결과는 세션 안에서 한 번만 안내한다. 미처리 구매는 복구 조회마다 다시 검증되므로,
    // 이 기록이 없으면 포그라운드 복귀 때마다 같은 안내가 쌓인다. 복구 조회 때 비우지 않고 세션이 바뀔 때만 비운다.
    private val announcedResults = mutableSetOf<Pair<String, PaymentEvent>>()

    // 다시 시도해도 결과가 같은 실패(계정 불일치·탈퇴 등)는 세션 안에서 다시 검증하지 않는다. 구매한 계정으로
    // 다시 로그인하면 세션이 바뀌어 기록이 비워지고 다시 검증한다.
    private val settledFailures = mutableSetOf<Pair<String, String>>()
    private var recoveryInProgress = false

    init {
        scope.launch {
            auth.sessionState.collectLatest { session ->
                attemptedPurchases.clear()
                announcedResults.clear()
                settledFailures.clear()
                mutableState.value = PaymentState()
                if (session == SessionState.Authenticated) {
                    recover()
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
        scope.launch { recover() }
    }

    private fun recover() {
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
                val purchase = outcome.purchaseToken to productId
                if (purchase !in settledFailures && attemptedPurchases.add(purchase)) {
                    verify(productId, outcome.purchaseToken)
                }
            }
            is BillingPurchaseOutcome.Pending -> showMessage("결제 대기 중입니다. 결제가 완료되면 다시 확인합니다.")
            BillingPurchaseOutcome.UserCanceled -> {
                mutableState.value = state.value.copy(message = null, billingError = null)
            }
            BillingPurchaseOutcome.AlreadyOwned -> showMessage("미처리 구매를 확인하고 있습니다.")
            is BillingPurchaseOutcome.Error -> {
                val updated = state.value.copy(message = null, billingError = outcome)
                // 사용자가 진행한 결제의 실패만 안내한다. 백그라운드 복구 조회 실패는 다음 복구 때 다시 조회한다.
                mutableState.value = if (outcome.source == BillingErrorSource.PURCHASE_UPDATE) {
                    updated.withNotice(PaymentEvent.PurchaseFailed)
                } else {
                    updated
                }
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
                    // 아직 표시하지 않은 이전 안내는 유지한다.
                    PaymentState(
                        paidBalance = grant.paidBalance,
                        message = "${grant.grantedTokens} 토큰 지급 완료",
                        notices = state.value.notices,
                    ).withNoticeOnce(purchaseToken, PaymentEvent.Granted(grant.grantedTokens))
                } else {
                    state.value.copy(isVerifying = false, message = "결제 대기 중입니다. 아직 토큰이 지급되지 않았습니다.")
                }
            }
            is AppResult.Failure -> {
                val reason = result.throwable.paymentFailureReason()
                if (reason != PaymentFailureReason.RETRYABLE) settledFailures.add(purchaseToken to productId)
                mutableState.value = state.value
                    .copy(isVerifying = false, message = result.throwable.paymentMessage())
                    .withNoticeOnce(purchaseToken, PaymentEvent.VerificationFailed(reason))
            }
        }
        attemptedPurchases.add(purchaseToken to productId)
    }

    override fun onNoticeShown(noticeId: Long) {
        scope.launch {
            mutableState.update { current -> current.copy(notices = current.notices.filterNot { it.id == noticeId }) }
        }
    }

    private fun showMessage(message: String) {
        mutableState.value = state.value.copy(message = message, billingError = null)
    }

    private fun PaymentState.withNoticeOnce(purchaseToken: String, event: PaymentEvent) =
        if (announcedResults.add(purchaseToken to event)) withNotice(event) else this

    private fun PaymentState.withNotice(event: PaymentEvent) =
        copy(notices = notices + PaymentNotice(id = nextNoticeId++, event = event))
}

sealed interface PaymentEvent {
    data class Granted(val grantedTokens: Long) : PaymentEvent
    data class VerificationFailed(val reason: PaymentFailureReason) : PaymentEvent

    /** Play 결제 자체가 실패했다(구매 콜백의 오류 응답). 사용자 취소는 포함하지 않는다. */
    data object PurchaseFailed : PaymentEvent
}

/**
 * [notices]는 UI가 한 번씩 안내해야 할 결과 목록이다. 일회성 이벤트 대신 상태로 두어, 화면이 없을 때
 * 발생한 안내도 남아 있다가 표시되고, 세션이 바뀌면 상태와 함께 초기화되어 다른 계정에 노출되지 않는다.
 */
data class PaymentState(
    val isVerifying: Boolean = false,
    val paidBalance: Long? = null,
    val message: String? = null,
    val billingError: BillingPurchaseOutcome.Error? = null,
    val notices: List<PaymentNotice> = emptyList(),
)

data class PaymentNotice(val id: Long, val event: PaymentEvent)

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
