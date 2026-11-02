package com.gamss.android.feature.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.gamss.android.domain.billing.PurchaseOutcome
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseResultMapperTest {

    @Test
    fun `OK + PURCHASED 는 Confirmed 로 변환된다`() {
        val purchase = fakePurchase(Purchase.PurchaseState.PURCHASED, "token-1", "pack_10")

        val outcomes = PurchaseResultMapper.fromPurchasesUpdated(okResult(), listOf(purchase))

        assertEquals(listOf(PurchaseOutcome.Confirmed(productId = "pack_10", purchaseToken = "token-1")), outcomes)
    }

    @Test
    fun `OK + PENDING 은 Pending 으로 변환된다`() {
        val purchase = fakePurchase(Purchase.PurchaseState.PENDING, "token-1", "pack_10")

        val outcomes = PurchaseResultMapper.fromPurchasesUpdated(okResult(), listOf(purchase))

        assertEquals(listOf(PurchaseOutcome.Pending), outcomes)
    }

    @Test
    fun `USER_CANCELED 는 purchases 와 무관하게 UserCanceled 로 변환된다`() {
        val outcomes = PurchaseResultMapper.fromPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.USER_CANCELED),
            purchases = null,
        )

        assertEquals(listOf(PurchaseOutcome.UserCanceled), outcomes)
    }

    @Test
    fun `ITEM_ALREADY_OWNED 는 AlreadyOwned 로 변환된다`() {
        val outcomes = PurchaseResultMapper.fromPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
            purchases = null,
        )

        assertEquals(listOf(PurchaseOutcome.AlreadyOwned), outcomes)
    }

    @Test
    fun `그 외 응답 코드는 Failed 로 변환된다`() {
        val outcomes = PurchaseResultMapper.fromPurchasesUpdated(
            billingResult(BillingClient.BillingResponseCode.ERROR),
            purchases = null,
        )

        assertEquals(1, outcomes.size)
        assertEquals(PurchaseOutcome.Failed::class, outcomes.single()::class)
    }

    @Test
    fun `복구 조회의 미소비 구매가 여러 건이면 모두 변환한다`() {
        val purchases = listOf(
            fakePurchase(Purchase.PurchaseState.PURCHASED, "token-1", "pack_10"),
            fakePurchase(Purchase.PurchaseState.PURCHASED, "token-2", "pack_30"),
        )

        val outcomes = PurchaseResultMapper.fromRecoveredPurchases(purchases)

        assertEquals(
            listOf(
                PurchaseOutcome.Confirmed(productId = "pack_10", purchaseToken = "token-1"),
                PurchaseOutcome.Confirmed(productId = "pack_30", purchaseToken = "token-2"),
            ),
            outcomes,
        )
    }

    private fun billingResult(code: Int): BillingResult = BillingResult.newBuilder().setResponseCode(code).build()

    private fun okResult(): BillingResult = billingResult(BillingClient.BillingResponseCode.OK)

    private fun fakePurchase(state: Int, token: String, productId: String): Purchase = mockk {
        every { purchaseState } returns state
        every { purchaseToken } returns token
        every { products } returns listOf(productId)
    }
}
