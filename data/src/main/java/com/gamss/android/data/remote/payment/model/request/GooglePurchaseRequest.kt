package com.gamss.android.data.remote.payment.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class GooglePurchaseRequest(val productId: String, val purchaseToken: String)