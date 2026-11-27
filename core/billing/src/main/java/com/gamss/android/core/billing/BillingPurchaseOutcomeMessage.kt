package com.gamss.android.core.billing

import com.gamss.android.domain.payment.BillingErrorSource
import com.gamss.android.domain.payment.BillingPurchaseOutcome

/** Play SDK 응답코드 등이 담긴 진단용 문구. 디버그 화면에서만 쓴다. */
fun BillingPurchaseOutcome.Error.paymentMessage(): String {
    val operation = when (source) {
        BillingErrorSource.PURCHASE_UPDATE -> "Google Play 결제 실패"
        BillingErrorSource.RESTORE_QUERY -> "Google Play 미처리 구매 조회 실패"
    }
    val detail = message.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
    return "$operation (코드: $responseCode)$detail"
}
