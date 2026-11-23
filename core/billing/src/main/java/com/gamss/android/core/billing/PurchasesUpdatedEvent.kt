package com.gamss.android.core.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

/** [com.android.billingclient.api.PurchasesUpdatedListener.onPurchasesUpdated] 콜백을 Flow로 흘려보내기 위한 이벤트. */
internal data class PurchasesUpdatedEvent(
    val billingResult: BillingResult,
    val purchases: List<Purchase>?,
)
