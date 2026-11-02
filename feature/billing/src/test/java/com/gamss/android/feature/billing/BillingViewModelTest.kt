package com.gamss.android.feature.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.billing.GetBillableProductsUseCase
import com.gamss.android.domain.billing.GrantResult
import com.gamss.android.domain.billing.VerifyAndGrantPurchaseQuery
import com.gamss.android.domain.billing.VerifyAndGrantPurchaseUseCase
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.user.GetUserInfoUseCase
import com.gamss.android.domain.user.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

/**
 * PurchasesUpdatedListener 콜백과 ITEM_ALREADY_OWNED 복구 조회가 같은 처리 경로를 타는지,
 * (BillingResponseCode, PurchaseState) 조합별로 올바르게 분기하는지를 확인한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModelTest {

    private val purchaseUpdatesFlow = MutableSharedFlow<PlayPurchaseUpdate>()
    private val userProfile = UserProfile(id = 1L, email = null, nickname = null, status = null, createdAt = null)

    private val getUserInfoUseCase: GetUserInfoUseCase = mockk()
    private val getBillableProducts: GetBillableProductsUseCase = mockk()
    private val verifyAndGrantPurchase: VerifyAndGrantPurchaseUseCase = mockk()
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier = mockk(relaxed = true)
    private val playBillingManager: PlayBillingManager = mockk()

    init {
        coEvery { getUserInfoUseCase.invoke() } returns AppResult.Success(userProfile)
        coEvery { getBillableProducts.invoke() } returns AppResult.Success(emptyList())
        every { playBillingManager.purchaseUpdates } returns purchaseUpdatesFlow
        coEvery { playBillingManager.queryOwnedPurchases() } returns emptyList()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `OK+PURCHASED 는 서버에 지급을 요청하고 성공하면 잔액을 재조회한다`() = runTest {
        coEvery { verifyAndGrantPurchase.invoke(any()) } returns AppResult.Success(GrantResult.Granted)

        viewModel().test(this) {
            purchaseUpdatesFlow.emit(
                PlayPurchaseUpdate(
                    okResult(),
                    listOf(fakePurchase(Purchase.PurchaseState.PURCHASED, "token-1", "pack_10")),
                ),
            )
            runCurrent()

            assertFalse(containerHost.container.stateFlow.value.isPurchaseInProgress)
            cancelAndIgnoreRemainingItems()
        }

        coVerify {
            verifyAndGrantPurchase.invoke(VerifyAndGrantPurchaseQuery(productId = "pack_10", purchaseToken = "token-1"))
        }
        coVerify(exactly = 1) { tokenUsageRefreshNotifier.requestRefresh() }
    }

    @Test
    fun `이미 지급됨 응답도 재조회를 트리거한다`() = runTest {
        coEvery { verifyAndGrantPurchase.invoke(any()) } returns AppResult.Success(GrantResult.AlreadyGranted)

        viewModel().test(this) {
            purchaseUpdatesFlow.emit(
                PlayPurchaseUpdate(
                    okResult(),
                    listOf(fakePurchase(Purchase.PurchaseState.PURCHASED, "token-1", "pack_10")),
                ),
            )
            runCurrent()
            cancelAndIgnoreRemainingItems()
        }

        coVerify(exactly = 1) { tokenUsageRefreshNotifier.requestRefresh() }
    }

    @Test
    fun `OK+PENDING 은 대기 안내만 표시하고 지급 요청은 하지 않는다`() = runTest {
        viewModel().test(this) {
            purchaseUpdatesFlow.emit(
                PlayPurchaseUpdate(
                    okResult(),
                    listOf(fakePurchase(Purchase.PurchaseState.PENDING, "token-1", "pack_10")),
                ),
            )
            runCurrent()

            assertTrue(containerHost.container.stateFlow.value.isPendingNoticeVisible)
            assertFalse(containerHost.container.stateFlow.value.isPurchaseInProgress)
            cancelAndIgnoreRemainingItems()
        }

        coVerify(exactly = 0) { verifyAndGrantPurchase.invoke(any()) }
        coVerify(exactly = 0) { tokenUsageRefreshNotifier.requestRefresh() }
    }

    @Test
    fun `USER_CANCELED 는 오류 안내 없이 구매 중 상태만 해제한다`() = runTest {
        val product = fakeBillingProduct("pack_10")

        viewModel().test(this) {
            containerHost.onPurchaseClick(product, product.options.first())
            runCurrent()
            assertTrue(containerHost.container.stateFlow.value.isPurchaseInProgress)

            purchaseUpdatesFlow.emit(
                PlayPurchaseUpdate(billingResult(BillingClient.BillingResponseCode.USER_CANCELED), null),
            )
            runCurrent()

            assertFalse(containerHost.container.stateFlow.value.isPurchaseInProgress)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `ITEM_ALREADY_OWNED 는 미소비 구매를 모두 다시 조회해 처리한다`() = runTest {
        coEvery { playBillingManager.queryOwnedPurchases() } returns listOf(
            fakePurchase(Purchase.PurchaseState.PURCHASED, "token-1", "pack_10"),
            fakePurchase(Purchase.PurchaseState.PURCHASED, "token-2", "pack_30"),
        )
        coEvery { verifyAndGrantPurchase.invoke(any()) } returns AppResult.Success(GrantResult.Granted)

        viewModel().test(this) {
            purchaseUpdatesFlow.emit(
                PlayPurchaseUpdate(billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED), null),
            )
            runCurrent()
            cancelAndIgnoreRemainingItems()
        }

        coVerify {
            verifyAndGrantPurchase.invoke(VerifyAndGrantPurchaseQuery(productId = "pack_10", purchaseToken = "token-1"))
        }
        coVerify {
            verifyAndGrantPurchase.invoke(VerifyAndGrantPurchaseQuery(productId = "pack_30", purchaseToken = "token-2"))
        }
        coVerify(exactly = 2) { tokenUsageRefreshNotifier.requestRefresh() }
    }

    private fun viewModel() = BillingViewModel(
        getUserInfoUseCase = getUserInfoUseCase,
        getBillableProducts = getBillableProducts,
        verifyAndGrantPurchase = verifyAndGrantPurchase,
        tokenUsageRefreshNotifier = tokenUsageRefreshNotifier,
        playBillingManager = playBillingManager,
    )

    private fun billingResult(code: Int): BillingResult = BillingResult.newBuilder().setResponseCode(code).build()

    private fun okResult(): BillingResult = billingResult(BillingClient.BillingResponseCode.OK)

    private fun fakePurchase(state: Int, token: String, productId: String): Purchase = mockk {
        every { purchaseState } returns state
        every { purchaseToken } returns token
        every { products } returns listOf(productId)
    }

    private fun fakeBillingProduct(productId: String): BillingProduct {
        val productDetails: ProductDetails = mockk {
            every { this@mockk.productId } returns productId
        }
        return BillingProduct(
            productId = productId,
            benefitDescription = "설명",
            productDetails = productDetails,
            options = listOf(BillingPurchaseOption(offerToken = "offer-1", formattedPrice = "₩1,000")),
        )
    }
}
