package com.gamss.android.app.payment

import com.gamss.android.core.billing.BillingConnectionState
import com.gamss.android.core.billing.BillingErrorSource
import com.gamss.android.core.billing.BillingPurchaseOutcome
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.payment.PaymentGrant
import com.gamss.android.domain.payment.PaymentStatus
import com.gamss.android.domain.payment.VerifyGooglePurchaseUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
    private val session = MutableStateFlow<SessionState>(SessionState.Authenticated)
    private val outcomes = Channel<BillingPurchaseOutcome>(Channel.UNLIMITED)

    private fun TestScope.verifier(): PendingPurchaseVerifier {
        every { auth.sessionState } returns session
        every { billing.purchaseResults } returns outcomes.receiveAsFlow()
        every { billing.connectionState } returns MutableStateFlow(BillingConnectionState.CONNECTED)
        return PendingPurchaseVerifier(billing, verifyGooglePurchase, auth, backgroundScope)
    }

    @Test
    fun `서버 지급 완료만 잔액에 반영한다`() = runTest {
        coEvery {
            verifyGooglePurchase(VerifyGooglePurchaseUseCase.Params("token_300000", "receipt"))
        } returns AppResult.Success(PaymentGrant(PaymentStatus.GRANTED, "token_300000", 300000, 600000))
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        assertEquals(600000L, verifier.state.value.paidBalance)
        assertEquals("300000 토큰 지급 완료", verifier.state.value.message)
    }

    @Test
    fun `서버 PENDING 응답은 지급 완료나 잔액으로 표시하지 않는다`() = runTest {
        coEvery { verifyGooglePurchase(any()) } returns AppResult.Success(
            PaymentGrant(PaymentStatus.PENDING, "token_300000", 0, 0),
        )
        val verifier = verifier()
        runCurrent()
        outcomes.send(purchased())
        runCurrent()
        assertNull(verifier.state.value.paidBalance)
        assertFalse(verifier.state.value.isVerifying)
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
        verifier.onResume()
        runCurrent()
        coVerify(exactly = 1) { billing.queryUnfinishedPurchases() }
        outcomes.send(purchased())
        runCurrent()
        coVerify(exactly = 2) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `Play 결제 대기는 서버에 지급 요청하지 않는다`() = runTest {
        verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Pending(listOf("token_300000")))
        runCurrent()
        coVerify(exactly = 0) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `로그아웃 후에는 복구 조회와 검증을 진행하지 않는다`() = runTest {
        val verifier = verifier()
        runCurrent()
        session.value = SessionState.Unauthenticated
        runCurrent()
        verifier.onResume()
        outcomes.send(purchased())
        runCurrent()
        coVerify(exactly = 0) { billing.queryUnfinishedPurchases() }
        coVerify(exactly = 0) { verifyGooglePurchase(any()) }
    }

    @Test
    fun `다음 복귀에는 이전 결제 오류를 지우고 새 복구 오류의 상세 정보를 유지한다`() = runTest {
        val verifier = verifier()
        runCurrent()
        outcomes.send(BillingPurchaseOutcome.Error(5, "previous checkout error"))
        runCurrent()
        assertEquals("previous checkout error", verifier.state.value.billingError?.message)

        verifier.onResume()
        runCurrent()
        assertNull(verifier.state.value.billingError)
        coVerify(exactly = 1) { billing.queryUnfinishedPurchases() }

        val recoveryError = BillingPurchaseOutcome.Error(5, "restore error", BillingErrorSource.RESTORE_QUERY)
        outcomes.send(recoveryError)
        runCurrent()
        assertEquals(recoveryError, verifier.state.value.billingError)
        assertEquals("Google Play 미처리 구매 조회 실패 (코드: 5)\nrestore error", recoveryError.paymentMessage())
        assertNull(verifier.state.value.paidBalance)
        coVerify(exactly = 0) { verifyGooglePurchase(any()) }
    }

    private fun purchased() = BillingPurchaseOutcome.Purchased("receipt", listOf("token_300000"))
}
