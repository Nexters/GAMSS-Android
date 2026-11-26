package com.gamss.android.domain.payment

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class GetPaymentProductsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) : NoParamUseCase<AppResult<List<TokenProduct>>> {

    override suspend fun invoke(): AppResult<List<TokenProduct>> = paymentRepository.getProducts()
}
