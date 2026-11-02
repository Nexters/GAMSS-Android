package com.gamss.android.feature.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.util.sha256
import com.gamss.android.domain.billing.GetBillableProductsUseCase
import com.gamss.android.domain.billing.GrantResult
import com.gamss.android.domain.billing.PurchaseOutcome
import com.gamss.android.domain.billing.VerifyAndGrantPurchaseQuery
import com.gamss.android.domain.billing.VerifyAndGrantPurchaseUseCase
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val getBillableProducts: GetBillableProductsUseCase,
    private val verifyAndGrantPurchase: VerifyAndGrantPurchaseUseCase,
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier,
    private val playBillingManager: PlayBillingManager,
) : ViewModel(), ContainerHost<BillingState, BillingSideEffect> {

    override val container = container<BillingState, BillingSideEffect>(BillingState())

    init {
        // PurchasesUpdatedListener 콜백과 ITEM_ALREADY_OWNED 복구 조회는 결국 같은 handleOutcome
        // 경로로 모인다 — 호출 출처를 구분하지 않는다.
        playBillingManager.purchaseUpdates
            .onEach { update -> onPurchaseUpdate(update) }
            .launchIn(viewModelScope)
    }

    fun loadProducts() = intent {
        reduce { state.copy(isLoadingProducts = true) }

        val serverProducts = when (val result = getBillableProducts()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                reduce { state.copy(isLoadingProducts = false) }
                postSideEffect(BillingSideEffect.ShowError(result.throwable.message))
                return@intent
            }
        }

        // 서버 목록에 있지만 Play 조회에 실패/누락된 상품은 mergeBillingProducts가
        // isPurchasable = false로 표시해 구매를 막는다.
        val productDetails = playBillingManager.queryProductDetails(serverProducts.map { it.productId })
        val merged = mergeBillingProducts(serverProducts, productDetails)
        reduce { state.copy(products = merged, isLoadingProducts = false) }
    }

    fun onPurchaseClick(product: BillingProduct, option: BillingPurchaseOption) = intent {
        if (state.isPurchaseInProgress) return@intent
        val productDetails = product.productDetails ?: return@intent

        reduce { state.copy(isPurchaseInProgress = true) }

        val userId = when (val result = getUserInfoUseCase()) {
            is AppResult.Success -> result.data.id
            is AppResult.Failure -> {
                reduce { state.copy(isPurchaseInProgress = false) }
                postSideEffect(BillingSideEffect.ShowError(result.throwable.message))
                return@intent
            }
        }

        postSideEffect(
            BillingSideEffect.LaunchPurchase(
                productDetails = productDetails,
                offerToken = option.offerToken,
                // 매 구매 시도마다 새로 만들지 않는다 — 계정 id에서 파생된 값이라 호출마다 같다.
                obfuscatedAccountId = userId.toString().sha256(),
            ),
        )
    }

    /**
     * Screen이 LaunchPurchase SideEffect를 받으면 LocalActivity로 이 함수를 호출한다.
     * viewModelScope.launch 는 기본적으로 Dispatchers.Main.immediate 에서 돌기 때문에
     * launchBillingFlow 가 메인 스레드에서 실행된다 — [ModelDownloadPromptViewModel.onConfirmDownload]
     * 와 같은 패턴.
     */
    fun launchPurchase(activity: Activity, effect: BillingSideEffect.LaunchPurchase) {
        viewModelScope.launch {
            val result = playBillingManager.launchBillingFlow(
                activity = activity,
                productDetails = effect.productDetails,
                offerToken = effect.offerToken,
                obfuscatedAccountId = effect.obfuscatedAccountId,
            )
            onBillingFlowLaunched(result)
        }
    }

    /**
     * launchBillingFlow의 즉시 반환값. OK는 결제창이 뜬 것일 뿐 결제 완료가 아니므로 구매 중
     * 상태를 그대로 둔다. 실제 결과는 [purchaseUpdates] 콜백으로 들어온다.
     */
    private fun onBillingFlowLaunched(result: BillingResult) = intent {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            reduce { state.copy(isPurchaseInProgress = false) }
            postSideEffect(BillingSideEffect.ShowError(result.debugMessage))
        }
    }

    fun dismissPendingNotice() = intent {
        reduce { state.copy(isPendingNoticeVisible = false) }
    }

    private fun onPurchaseUpdate(update: PlayPurchaseUpdate) = intent {
        handleOutcomes(PurchaseResultMapper.fromPurchasesUpdated(update.billingResult, update.purchases))
    }

    private suspend fun Syntax<BillingState, BillingSideEffect>.handleOutcomes(outcomes: List<PurchaseOutcome>) {
        outcomes.forEach { outcome -> handleOutcome(outcome) }
    }

    private suspend fun Syntax<BillingState, BillingSideEffect>.handleOutcome(outcome: PurchaseOutcome) {
        when (outcome) {
            is PurchaseOutcome.Confirmed -> handleConfirmed(outcome)

            PurchaseOutcome.Pending -> reduce {
                state.copy(isPurchaseInProgress = false, isPendingNoticeVisible = true)
            }

            // 오류 안내 없이 구매 중 상태만 해제한다.
            PurchaseOutcome.UserCanceled -> reduce { state.copy(isPurchaseInProgress = false) }

            // 미소비 구매가 한 건이라고 단정하지 않고 다시 조회해 전부 복구 처리한다.
            PurchaseOutcome.AlreadyOwned -> {
                val recovered = playBillingManager.queryOwnedPurchases()
                handleOutcomes(PurchaseResultMapper.fromRecoveredPurchases(recovered))
            }

            is PurchaseOutcome.Failed -> {
                reduce { state.copy(isPurchaseInProgress = false) }
                postSideEffect(BillingSideEffect.ShowError(outcome.message))
            }
        }
    }

    private suspend fun Syntax<BillingState, BillingSideEffect>.handleConfirmed(outcome: PurchaseOutcome.Confirmed) {
        val query = VerifyAndGrantPurchaseQuery(productId = outcome.productId, purchaseToken = outcome.purchaseToken)
        when (val result = verifyAndGrantPurchase(query)) {
            is AppResult.Success -> {
                // Granted/AlreadyGranted 모두 재조회를 트리거한다 — 멱등 응답이라고 갱신을 건너뛰지 않는다.
                when (result.data) {
                    is GrantResult.Granted, is GrantResult.AlreadyGranted -> tokenUsageRefreshNotifier.requestRefresh()
                }
                reduce { state.copy(isPurchaseInProgress = false) }
            }

            is AppResult.Failure -> {
                reduce { state.copy(isPurchaseInProgress = false) }
                postSideEffect(BillingSideEffect.ShowError(result.throwable.message))
            }
        }
    }
}
