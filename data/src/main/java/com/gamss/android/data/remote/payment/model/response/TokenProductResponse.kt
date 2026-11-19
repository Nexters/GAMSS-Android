package com.gamss.android.data.remote.payment.model.response

import kotlinx.serialization.Serializable

@Serializable
internal data class TokenProductResponse(val productId: String, val tokenAmount: Long)