package com.gamss.android.domain.payment

import javax.inject.Inject

class ConnectBillingUseCase @Inject constructor(
    private val billingRepository: StoreBillingRepository,
) {
    operator fun invoke() = billingRepository.connect()
}
