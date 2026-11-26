package com.gamss.android.domain.payment

import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class GetBillingProductsUseCase @Inject constructor(
    private val billingRepository: StoreBillingRepository,
) : UseCase<List<String>, StoreProductQueryResult> {
    override suspend fun invoke(params: List<String>): StoreProductQueryResult = billingRepository.getProducts(params)
}
