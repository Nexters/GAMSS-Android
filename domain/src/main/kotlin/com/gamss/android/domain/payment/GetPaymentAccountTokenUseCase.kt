package com.gamss.android.domain.payment

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class GetPaymentAccountTokenUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) : NoParamUseCase<AppResult<String>> {

    override suspend fun invoke(): AppResult<String> = paymentRepository.getAccountToken()
}
