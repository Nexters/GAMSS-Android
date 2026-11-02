package com.gamss.android.domain.billing

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class GetBillableProductsUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) : NoParamUseCase<AppResult<List<BillingProductInfo>>> {

    override suspend fun invoke(): AppResult<List<BillingProductInfo>> =
        billingRepository.getServerProducts()
}
