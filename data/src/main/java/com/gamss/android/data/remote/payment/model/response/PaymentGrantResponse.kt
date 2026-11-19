package com.gamss.android.data.remote.payment.model.response

import kotlinx.serialization.Serializable

@Serializable
internal data class PaymentGrantResponse(
    val status: String,
    val productId: String,
    val grantedTokens: Long = 0,
    val paidBalance: Long = 0,
)
