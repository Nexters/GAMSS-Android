package com.gamss.android.core.billing

import com.android.billingclient.api.BillingResult

/** [com.android.billingclient.api.BillingClientStateListener]의 두 콜백을 Flow로 흘려보내기 위한 이벤트. */
internal sealed interface BillingConnectionEvent {
    data class SetupFinished(val billingResult: BillingResult) : BillingConnectionEvent
    data object ServiceDisconnected : BillingConnectionEvent
}
