package com.gamss.android.core.billing

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayBillingRepositoryTest {

    private val fakeDataSource = FakeBillingDataSource()
    private lateinit var repository: PlayBillingRepository

    @Before
    fun setUp() {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        repository = PlayBillingRepository(fakeDataSource, scope)
    }

    @Test
    fun `connect는 이미 연결 중이거나 연결됐으면 다시 startConnection을 호출하지 않는다`() {
        repository.connect()
        repository.connect() // 이미 CONNECTING 상태라 재호출되지 않아야 한다.
        assertEquals(1, fakeDataSource.startConnectionCallCount)

        fakeDataSource.emitConnectionEvent(BillingConnectionEvent.SetupFinished(billingResult(BillingResponseCode.OK)))
        repository.connect() // 이미 CONNECTED 상태라 재호출되지 않아야 한다.
        assertEquals(1, fakeDataSource.startConnectionCallCount)
    }

    @Test
    fun `연결 성공 시 CONNECTED가 되고 미처리 구매를 자동으로 재조회한다`() {
        fakeDataSource.onStartConnection = {
            val event = BillingConnectionEvent.SetupFinished(billingResult(BillingResponseCode.OK))
            fakeDataSource.emitConnectionEvent(event)
        }

        repository.connect()

        assertEquals(BillingConnectionState.CONNECTED, repository.connectionState.value)
        assertEquals(1, fakeDataSource.queryPurchasesCallCount)
    }

    @Test
    fun `연결 실패 시 DISCONNECTED로 남는다`() {
        fakeDataSource.onStartConnection = {
            fakeDataSource.emitConnectionEvent(
                BillingConnectionEvent.SetupFinished(billingResult(BillingResponseCode.BILLING_UNAVAILABLE)),
            )
        }

        repository.connect()

        assertEquals(BillingConnectionState.DISCONNECTED, repository.connectionState.value)
    }

    @Test
    fun `queryProductDetails는 호출할 때마다 Play에 새로 조회한다 - 캐시하지 않는다`() = runTest {
        repository.queryProductDetails(listOf("p1"))
        repository.queryProductDetails(listOf("p1"))

        assertEquals(2, fakeDataSource.queryProductDetailsCallCount)
    }

    @Test
    fun `queryProductDetails 실패 응답의 responseCode를 그대로 전달한다`() = runTest {
        fakeDataSource.queryProductDetailsResult =
            billingResult(BillingResponseCode.SERVICE_DISCONNECTED) to
            QueryProductDetailsResult.create(emptyList(), emptyList())

        val result = repository.queryProductDetails(listOf("p1"))

        assertEquals(BillingResponseCode.SERVICE_DISCONNECTED, result.callResult.responseCode)
        assertEquals(false, result.callResult.isSuccess)
    }

    @Test
    fun `구매 콜백 결과는 purchaseResults로 흘러간다`() = runTest {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.products } returns listOf("p1")
        every { purchase.purchaseToken } returns "token-1"

        val received = mutableListOf<BillingPurchaseOutcome>()
        val job = launch { repository.purchaseResults.collect { received += it } }
        runCurrent()

        val event = PurchasesUpdatedEvent(billingResult(BillingResponseCode.OK), listOf(purchase))
        fakeDataSource.emitPurchaseUpdate(event)
        runCurrent()

        assertEquals(
            listOf(BillingPurchaseOutcome.Purchased(purchaseToken = "token-1", productIds = listOf("p1"))),
            received,
        )
        job.cancel()
    }

    @Test
    fun `queryUnfinishedPurchases 복구 결과도 구매 콜백과 동일한 purchaseResults 경로로 흘러간다`() = runTest {
        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PENDING
        every { purchase.products } returns listOf("p1")
        every { purchase.purchaseToken } returns "token-1"
        fakeDataSource.queryPurchasesResult = PurchasesResult(billingResult(BillingResponseCode.OK), listOf(purchase))

        val received = mutableListOf<BillingPurchaseOutcome>()
        val job = launch { repository.purchaseResults.collect { received += it } }
        runCurrent()

        repository.queryUnfinishedPurchases()
        runCurrent()

        assertEquals(listOf(BillingPurchaseOutcome.Pending(productIds = listOf("p1"))), received)
        job.cancel()
    }

    @Test
    fun `ITEM_ALREADY_OWNED 콜백을 받으면 미처리 구매를 다시 조회한다 - 하나뿐이라고 단정하지 않는다`() = runTest {
        val event = PurchasesUpdatedEvent(billingResult(BillingResponseCode.ITEM_ALREADY_OWNED), null)
        fakeDataSource.emitPurchaseUpdate(event)
        runCurrent()

        assertEquals(1, fakeDataSource.queryPurchasesCallCount)
    }

    @Test
    fun `consumePurchase는 Play의 응답 코드를 그대로 반환한다`() = runTest {
        fakeDataSource.consumePurchaseResult = ConsumeResult(billingResult(BillingResponseCode.OK), "token-1")

        val result = repository.consumePurchase("token-1")

        assertEquals(BillingResponseCode.OK, result.responseCode)
        assertEquals(true, result.isSuccess)
    }

    private fun billingResult(responseCode: Int, debugMessage: String = ""): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(responseCode)
            .setDebugMessage(debugMessage)
            .build()
}
