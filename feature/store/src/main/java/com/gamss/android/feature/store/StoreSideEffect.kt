package com.gamss.android.feature.store

import com.gamss.android.domain.payment.StoreProduct

sealed interface StoreSideEffect {
    data object NavigateToSetting : StoreSideEffect
    data class LaunchBillingFlow(val product: StoreProduct, val accountToken: String) : StoreSideEffect
}
