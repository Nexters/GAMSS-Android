package com.gamss.android.core.billing

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.payment.PaymentGrant
import com.gamss.android.domain.payment.PaymentStatus
import com.gamss.android.domain.payment.VerifyGooglePurchaseUseCase
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PendingPurchaseVerifierTest {
    private val billing = mockk<BillingRepository>(relaxed = true)
    private val verifyGooglePurchase = mockk<VerifyGooglePurchaseUseCase>()
    private val auth = mockk<AuthRepository>()
    private val notifier = mockk<TokenUsageRefreshNotifier>(relaxed = true)
    private val session = MutableStateFlow<SessionState>(SessionState.Authenticated)
    private val outcomes = Channel<BillingPurchaseOutcome>(Channel.UNLIMITED)

    private fun TestScope.verifier(): PendingPurchaseVerifier {
        every { auth.sessionState } returns session
        every { billing.purchaseResults } returns outcomes.receiveAsFlow()
        every { billing.connectionState } returns MutableStateFlow(BillingConnectionState.CONNECTED)
        return PendingPurchaseVerifierImpl(billing, verifyGooglePurchase, auth, notifier, backgroundScope)
    }

    @Test
    fun `서버 지급 완료만 잔액에 반영한다`() = runTest {
        coEvery {
            verifyGooglePurchase(VerifyGooglePurchaseUseCase.Params("token_300k", "receipt"))
        } returns AppResult.Success(PaymentGrant(PaymentStatus.GRANTED, "token_300k", 300000, 600000))
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        assertEquals(600000L, verifier.state.value.paidBalance)
        assertEquals("300000 토큰 지급 완료", verifier.state.value.message)
        verify(exactly = 1) { notifier.requestRefresh() }
    }

    @Test
    fun `서버 PENDING 응답은 지급 완료나 잔액으로 표시하지 않는다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returns AppResult.Success(
            PaymentGrant(PaymentStatus.PENDING, "token_300k", 0, 0),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        assertNull(verifier.state.value.paidBalance)
        assertFalse(verifier.state.value.isVerifying)
        verify(exactly = 0) { notifier.requestRefresh() }
    }

    @Test
    fun `403 중복 콜백은 즉시 재시도하지 않고 다음 복귀 때 다시 검증한다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returns AppResult.Failure(
            ApiException.Http(403, "PURCHASE_ACCESS_DENIED", "denied"),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        outcomes.send(purchased())
        runCurrent()
        coVerify(exactly = 1) { verifyGooglePurchase(any()) }
        verifier.requestRecovery()
        runCurrent()
        coVerify(exactly = 2) { billing.queryUnfinishedPurchases() }
        outcomes.send(purchased())
        runCurrent()
        coVerify(exactly = 2) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `Play 결제 대기는 서버에 지급 요청하지 않는다`() = runTest {
        verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Pending(listOf("token_300k")))
        runCurrent()
        coVerify(exactly = 0) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `검증 중 복귀는 같은 구매를 즉시 재검증하지 않고 다음 복귀에서 재시도한다`() = runTest {
        val pending = CompletableDeferred<AppResult<PaymentGrant>>()
        coEvery { verifyGooglePurchase(any()) } coAnswers { pending.await() }
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        verifier.requestRecovery()
        runCurrent()
        outcomes.send(purchased())
        pending.complete(AppResult.Failure(IllegalStateException()))
        runCurrent()
        coVerify(exactly = 1) { verifyGooglePurchase(any()) }
        verifier.requestRecovery()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        coVerify(exactly = 2) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `로그아웃 후에는 복구 조회와 검증을 진행하지 않는다`() = runTest {
        val verifier = verifier()
        runCurrent()
        session.value = SessionState.Unauthenticated
        runCurrent()
        verifier.requestRecovery()
        outcomes.send(purchased())
        runCurrent()
        coVerify(exactly = 1) { billing.queryUnfinishedPurchases() }
        coVerify(exactly = 0) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `다음 복귀에는 이전 결제 오류를 지우고 새 복구 오류의 상세 정보를 유지한다`() = runTest {
        val verifier = verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Error(5, "previous checkout error"))
        runCurrent()
        assertEquals("previous checkout error", verifier.state.value.billingError?.message)

        verifier.requestRecovery()
        runCurrent()
        assertNull(verifier.state.value.billingError)
        coVerify(exactly = 2) { billing.queryUnfinishedPurchases() }

        val recoveryError = BillingPurchaseOutcome.Error(5, "restore error", BillingErrorSource.RESTORE_QUERY)
        outcomes.send(recoveryError)
        runCurrent()
        assertEquals(recoveryError, verifier.state.value.billingError)
        assertEquals("Google Play 미처리 구매 조회 실패 (코드: 5)\nrestore error", recoveryError.paymentMessage())
        assertNull(verifier.state.value.paidBalance)
        coVerify(exactly = 0) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `서버 지급 완료와 검증 실패를 표시 전까지 유지되는 안내로 쌓는다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returnsMany listOf(
            AppResult.Success(PaymentGrant(PaymentStatus.GRANTED, "token_300k", 300000, 600000)),
            AppResult.Failure(IllegalStateException()),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Purchased("receipt-1", listOf("token_300k")))
        outcomes.send(BillingPurchaseOutcome.Purchased("receipt-2", listOf("token_300k")))
        runCurrent()

        assertEquals(
            listOf(
                PaymentEvent.Granted(300000),
                PaymentEvent.VerificationFailed(PaymentFailureReason.RETRYABLE),
            ),
            verifier.state.value.notices.map { it.event },
        )
    }

    @Test
    fun `표시한 안내만 제거하고 나머지는 유지한다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returnsMany listOf(
            AppResult.Success(PaymentGrant(PaymentStatus.GRANTED, "token_300k", 300000, 600000)),
            AppResult.Failure(IllegalStateException()),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Purchased("receipt-1", listOf("token_300k")))
        outcomes.send(BillingPurchaseOutcome.Purchased("receipt-2", listOf("token_300k")))
        runCurrent()

        verifier.onNoticeShown(verifier.state.value.notices.first().id)
        runCurrent()

        assertEquals(
            listOf(PaymentEvent.VerificationFailed(PaymentFailureReason.RETRYABLE)),
            verifier.state.value.notices.map { it.event },
        )
    }

    @Test
    fun `세션이 바뀌면 이전 계정의 안내는 표시하지 않고 비운다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returns AppResult.Success(
            PaymentGrant(PaymentStatus.GRANTED, "token_300k", 300000, 600000),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        assertEquals(1, verifier.state.value.notices.size)

        session.value = SessionState.Unauthenticated
        runCurrent()
        session.value = SessionState.Authenticated
        runCurrent()

        assertEquals(emptyList<PaymentNotice>(), verifier.state.value.notices)
    }

    @Test
    fun `다른 스레드에서 복구를 요청해도 호출 스레드가 아닌 verifier 스코프에서 처리한다`() = runTest {
        val verifier = verifier()
        runCurrent()
        coVerify(exactly = 1) { billing.queryUnfinishedPurchases() }

        // Orbit intent처럼 메인이 아닌 스레드에서 호출되는 상황.
        val caller = Thread { verifier.requestRecovery() }
        caller.start()
        caller.join()
        coVerify(exactly = 1) { billing.queryUnfinishedPurchases() }

        runCurrent()
        coVerify(exactly = 2) { billing.queryUnfinishedPurchases() }
    }

    @Test
    fun `사용자가 진행한 Play 결제 실패만 안내하고 백그라운드 복구 조회 실패는 안내하지 않는다`() = runTest {
        val verifier = verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Error(3, "unavailable", BillingErrorSource.RESTORE_QUERY))
        runCurrent()
        assertEquals(emptyList<PaymentNotice>(), verifier.state.value.notices)

        outcomes.send(BillingPurchaseOutcome.Error(3, "unavailable", BillingErrorSource.PURCHASE_UPDATE))
        runCurrent()
        assertEquals(listOf(PaymentEvent.PurchaseFailed), verifier.state.value.notices.map { it.event })
    }

    @Test
    fun `서버 PENDING 응답은 완료나 실패 안내를 쌓지 않는다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returns AppResult.Success(
            PaymentGrant(PaymentStatus.PENDING, "token_300k", 0, 0),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        assertEquals(emptyList<PaymentNotice>(), verifier.state.value.notices)
    }

    @Test
    fun `서버 오류 코드를 다시 시도로 해결되는지 기준으로 분류한다`() {
        val expected = mapOf(
            ApiException.Network(IllegalStateException()) to PaymentFailureReason.RETRYABLE,
            ApiException.Http(503, "PAYMENT_STORE_UNAVAILABLE", "") to PaymentFailureReason.RETRYABLE,
            ApiException.Http(500, "UNKNOWN", "") to PaymentFailureReason.RETRYABLE,
            ApiException.Http(403, "PURCHASE_ACCESS_DENIED", "") to PaymentFailureReason.ACCOUNT_MISMATCH,
            ApiException.Http(403, "WITHDRAWN_MEMBER", "") to PaymentFailureReason.WITHDRAWN_MEMBER,
            ApiException.Http(409, "PURCHASE_CANCELLED", "") to PaymentFailureReason.PURCHASE_CANCELLED,
            ApiException.Http(409, "PAYMENT_PRODUCT_UNAVAILABLE", "") to PaymentFailureReason.PRODUCT_UNAVAILABLE,
            ApiException.Http(400, "PURCHASE_QUANTITY_UNSUPPORTED", "") to PaymentFailureReason.INVALID_PURCHASE,
            ApiException.Http(400, "PURCHASE_INVALID", "") to PaymentFailureReason.INVALID_PURCHASE,
        )
        expected.forEach { (throwable, reason) -> assertEquals(reason, throwable.paymentFailureReason()) }
    }

    private fun purchased() = BillingPurchaseOutcome.Purchased("receipt", listOf("token_300k"))
}
