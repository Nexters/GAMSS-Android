package com.gamss.android.core.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener

/**
 * BillingClient.Builder.setListener()는 BillingClient 생성 시점에 리스너를 필요로 하지만,
 * 실제 처리를 담당하는 [PlayBillingRepository]는 그 BillingClient를 생성자로 주입받는 쪽이라
 * (테스트에서 fake BillingClient로 바꿀 수 있어야 하므로) 생성 순서가 꼬인다. 이 빈 껍데기를
 * BillingClient 생성 시 먼저 리스너로 꽂아두고, [PlayBillingRepository]가 생성되며 자신을
 * [delegate]로 등록해 간접적으로 콜백을 받는다.
 */
internal class ForwardingPurchasesUpdatedListener : PurchasesUpdatedListener {
    var delegate: PurchasesUpdatedListener? = null

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        delegate?.onPurchasesUpdated(billingResult, purchases)
    }
}
