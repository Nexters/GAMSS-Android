package com.gamss.android.app.payment

import javax.inject.Inject

class ObservePaymentStateUseCase @Inject constructor(
    private val verifier: PendingPurchaseVerifier,
) {
    operator fun invoke() = verifier.state
}
