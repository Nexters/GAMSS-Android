package com.gamss.android.core.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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

    private val billingClient = mockk<BillingClient>()
    private val forwardingListener = ForwardingPurchasesUpdatedListener()
    private lateinit var repository: PlayBillingRepository

    @Before
    fun setUp() {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        repository = PlayBillingRepository(billingClient, forwardingListener, scope)
        // connect() 성공·ITEM_ALREADY_OWNED 복구 등에서 내부적으로 자동 호출되는
        // queryUnfinishedPurchases()가 매번 걸려 넘어지지 않도록 기본 응답을 깔아 둔다.
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>()
                .onQueryPurchasesResponse(billingResult(BillingResponseCode.OK), emptyList())
        }
    }

    @Test
    fun `connect는 이미 연결 중이거나 연결됐으면 다시 startConnection을 호출하지 않는다`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } just Runs

        repository.connect()
        repository.connect() // 이미 CONNECTING 상태라 재호출되지 않아야 한다.
        verify(exactly = 1) { billingClient.startConnection(any()) }

        listenerSlot.captured.onBillingSetupFinished(billingResult(BillingResponseCode.OK))
        repository.connect() // 이미 CONNECTED 상태라 재호출되지 않아야 한다.
        verify(exactly = 1) { billingClient.startConnection(any()) }
    }

    @Test
    fun `연결 성공 시 CONNECTED가 되고 미처리 구매를 자동으로 재조회한다`() {
        every { billingClient.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>().onBillingSetupFinished(billingResult(BillingResponseCode.OK))
        }

        repository.connect()

        assertEquals(BillingConnectionState.CONNECTED, repository.connectionState.value)
        verify(exactly = 1) { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    @Test
    fun `연결 실패 시 DISCONNECTED로 남는다`() {
        every { billingClient.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>()
                .onBillingSetupFinished(billingResult(BillingResponseCode.BILLING_UNAVAILABLE))
        }

        repository.connect()

        assertEquals(BillingConnectionState.DISCONNECTED, repository.connectionState.value)
    }

    @Test
    fun `queryProductDetails는 호출할 때마다 Play에 새로 조회한다 - 캐시하지 않는다`() = runTest {
        every { billingClient.queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(
                billingResult(BillingResponseCode.OK),
                QueryProductDetailsResult.create(emptyList(), emptyList()),
            )
        }

        repository.queryProductDetails(listOf("p1"))
        repository.queryProductDetails(listOf("p1"))

        verify(exactly = 2) { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) }
    }

    @Test
    fun `queryProductDetails 실패 응답의 billingResult를 그대로 전달한다`() = runTest {
        every { billingClient.queryProductDetailsAsync(any(), any()) } answers {
            secondArg<ProductDetailsResponseListener>().onProductDetailsResponse(
                billingResult(BillingResponseCode.SERVICE_DISCONNECTED),
                QueryProductDetailsResult.create(emptyList(), emptyList()),
            )
        }

        val result = repository.queryProductDetails(listOf("p1"))

        assertEquals(BillingResponseCode.SERVICE_DISCONNECTED, result.billingResult.responseCode)
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

        repository.onPurchasesUpdated(billingResult(BillingResponseCode.OK), listOf(purchase))
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
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>()
                .onQueryPurchasesResponse(billingResult(BillingResponseCode.OK), listOf(purchase))
        }

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
        repository.onPurchasesUpdated(billingResult(BillingResponseCode.ITEM_ALREADY_OWNED), purchases = null)
        runCurrent()

        verify(exactly = 1) { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    @Test
    fun `consumePurchase는 Play의 BillingResult를 그대로 반환한다`() = runTest {
        every { billingClient.consumeAsync(any(), any()) } answers {
            secondArg<ConsumeResponseListener>().onConsumeResponse(billingResult(BillingResponseCode.OK), "token-1")
        }

        val result = repository.consumePurchase("token-1")

        assertEquals(BillingResponseCode.OK, result.responseCode)
    }

    private fun billingResult(responseCode: Int, debugMessage: String = ""): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(responseCode)
            .setDebugMessage(debugMessage)
            .build()
}
