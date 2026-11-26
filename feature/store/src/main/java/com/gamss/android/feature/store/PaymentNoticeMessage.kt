package com.gamss.android.feature.store

import androidx.annotation.StringRes
import com.gamss.android.core.billing.PaymentEvent
import com.gamss.android.core.billing.PaymentFailureReason

/** 결제 결과 안내 문구. 다시 시도로 해결되는 실패만 재시도 문구를 쓰고, 나머지는 원인을 그대로 안내한다. */
@StringRes
fun PaymentEvent.messageRes(): Int = when (this) {
    is PaymentEvent.Granted -> R.string.store_purchase_complete_toast
    PaymentEvent.PurchaseFailed -> R.string.store_purchase_failed_toast
    is PaymentEvent.VerificationFailed -> when (reason) {
        PaymentFailureReason.RETRYABLE -> R.string.store_purchase_verify_fail_toast
        PaymentFailureReason.LOGIN_REQUIRED -> R.string.store_purchase_login_required_toast
        PaymentFailureReason.ACCOUNT_MISMATCH -> R.string.store_purchase_account_mismatch_toast
        PaymentFailureReason.WITHDRAWN_MEMBER -> R.string.store_purchase_withdrawn_member_toast
        PaymentFailureReason.PURCHASE_CANCELLED -> R.string.store_purchase_cancelled_toast
        PaymentFailureReason.PRODUCT_UNAVAILABLE -> R.string.store_purchase_product_unavailable_toast
        PaymentFailureReason.INVALID_PURCHASE -> R.string.store_purchase_invalid_toast
    }
}
