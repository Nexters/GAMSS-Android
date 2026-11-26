package com.gamss.android.domain.payment

import javax.inject.Inject

class ObserveBillingConnectionUseCase @Inject constructor(
    private val billingRepository: StoreBillingRepository,
) {
    operator fun invoke() = billingRepository.connectionState
}
