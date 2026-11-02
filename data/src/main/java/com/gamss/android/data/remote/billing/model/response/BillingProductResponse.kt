package com.gamss.android.data.remote.billing.model.response

import com.gamss.android.domain.billing.BillingProductInfo
import kotlinx.serialization.Serializable

@Serializable
internal data class BillingProductResponse(
    val productId: String,
    val benefitDescription: String = "",
)

internal fun BillingProductResponse.toDomain(): BillingProductInfo =
    BillingProductInfo(productId = productId, benefitDescription = benefitDescription)
