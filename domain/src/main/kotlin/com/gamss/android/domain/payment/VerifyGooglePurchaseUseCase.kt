package com.gamss.android.domain.payment

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class VerifyGooglePurchaseUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
) : UseCase<VerifyGooglePurchaseUseCase.Params, AppResult<PaymentGrant>> {

    data class Params(val productId: String, val purchaseToken: String)

    override suspend fun invoke(params: Params): AppResult<PaymentGrant> =
        paymentRepository.verifyGooglePurchase(params.productId, params.purchaseToken)
}
