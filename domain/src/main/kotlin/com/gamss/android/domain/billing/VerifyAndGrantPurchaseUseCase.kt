package com.gamss.android.domain.billing

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

data class VerifyAndGrantPurchaseQuery(
    val productId: String,
    val purchaseToken: String,
)

class VerifyAndGrantPurchaseUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) : UseCase<VerifyAndGrantPurchaseQuery, AppResult<GrantResult>> {

    override suspend fun invoke(params: VerifyAndGrantPurchaseQuery): AppResult<GrantResult> =
        billingRepository.verifyAndGrantPurchase(params.productId, params.purchaseToken)
}
