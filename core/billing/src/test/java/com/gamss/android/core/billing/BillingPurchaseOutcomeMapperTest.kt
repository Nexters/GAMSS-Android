package com.gamss.android.core.billing

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingPurchaseOutcomeMapperTest {

    @Test
    fun `OK 응답과 PURCHASED 상태의 구매는 Purchased로 매핑된다`() {
        val purchase = fakePurchase(
            state = Purchase.PurchaseState.PURCHASED,
            token = "token-1",
            products = listOf("p1"),
        )

        val outcomes = mapToPurchaseOutcomes(billingResult(BillingResponseCode.OK), listOf(purchase))

        val expected = BillingPurchaseOutcome.Purchased(purchaseToken = "token-1", productIds = listOf("p1"))
        assertEquals(listOf(expected), outcomes)
    }

    @Test
    fun `OK 응답과 PENDING 상태의 구매는 Pending으로 매핑된다`() {
        val purchase = fakePurchase(state = Purchase.PurchaseState.PENDING, products = listOf("p1"))

        val outcomes = mapToPurchaseOutcomes(billingResult(BillingResponseCode.OK), listOf(purchase))

        assertEquals(listOf(BillingPurchaseOutcome.Pending(productIds = listOf("p1"))), outcomes)
    }

    @Test
    fun `OK 응답에 여러 구매가 섞여 있으면 각자 매핑된다 - 미소비 구매가 하나라고 단정하지 않는다`() {
        val purchased = fakePurchase(
            state = Purchase.PurchaseState.PURCHASED,
            token = "token-a",
            products = listOf("a"),
        )
        val pending = fakePurchase(state = Purchase.PurchaseState.PENDING, products = listOf("b"))

        val outcomes = mapToPurchaseOutcomes(billingResult(BillingResponseCode.OK), listOf(purchased, pending))

        assertEquals(2, outcomes.size)
        assertTrue(outcomes[0] is BillingPurchaseOutcome.Purchased)
        assertTrue(outcomes[1] is BillingPurchaseOutcome.Pending)
    }

    @Test
    fun `USER_CANCELED는 구매 목록과 무관하게 UserCanceled 하나로 매핑된다`() {
        val outcomes = mapToPurchaseOutcomes(billingResult(BillingResponseCode.USER_CANCELED), purchases = null)

        assertEquals(listOf(BillingPurchaseOutcome.UserCanceled), outcomes)
    }

    @Test
    fun `ITEM_ALREADY_OWNED는 AlreadyOwned로 매핑된다`() {
        val outcomes = mapToPurchaseOutcomes(billingResult(BillingResponseCode.ITEM_ALREADY_OWNED), purchases = null)

        assertEquals(listOf(BillingPurchaseOutcome.AlreadyOwned(productIds = emptyList())), outcomes)
    }

    @Test
    fun `그 외 오류 응답코드는 Error로 매핑된다`() {
        val outcomes = mapToPurchaseOutcomes(
            billingResult(BillingResponseCode.SERVICE_DISCONNECTED, debugMessage = "disconnected"),
            purchases = null,
        )

        val expected = BillingPurchaseOutcome.Error(
            responseCode = BillingResponseCode.SERVICE_DISCONNECTED,
            message = "disconnected",
        )
        assertEquals(listOf(expected), outcomes)
    }

    private fun billingResult(responseCode: Int, debugMessage: String = ""): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(responseCode)
            .setDebugMessage(debugMessage)
            .build()

    private fun fakePurchase(state: Int, products: List<String>, token: String = "token"): Purchase {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns state
        every { purchase.products } returns products
        every { purchase.purchaseToken } returns token
        every { purchase.orderId } returns "order-id"
        return purchase
    }
}
