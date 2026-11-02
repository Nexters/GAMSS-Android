package com.gamss.android.feature.billing

data class BillingState(
    val products: List<BillingProduct> = emptyList(),
    val isLoadingProducts: Boolean = false,
    val isPurchaseInProgress: Boolean = false,
    /** OK+PENDING 결과를 받았을 때만 켜진다. 토큰은 지급하지 않는다. */
    val isPendingNoticeVisible: Boolean = false,
)
