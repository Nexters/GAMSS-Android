package com.gamss.android.data.remote.billing.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class VerifyPurchaseRequest(
    val productId: String,
    val purchaseToken: String,
)
