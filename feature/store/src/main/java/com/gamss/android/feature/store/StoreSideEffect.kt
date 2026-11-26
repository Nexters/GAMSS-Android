package com.gamss.android.feature.store

import androidx.annotation.StringRes
import com.gamss.android.domain.payment.StoreProduct

sealed interface StoreSideEffect {
    data object NavigateToSetting : StoreSideEffect
    data class LaunchBillingFlow(val product: StoreProduct, val accountToken: String) : StoreSideEffect
    data class ShowToast(@StringRes val messageRes: Int) : StoreSideEffect
}
