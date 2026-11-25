package com.gamss.android.core.billing

import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.auth.SessionExpiredException

fun Throwable.paymentMessage(): String = when (this) {
    is SessionExpiredException -> "로그인이 필요합니다. 다시 로그인해 주세요."
    is ApiException.Network -> "서버에 연결할 수 없습니다. 잠시 후 다시 확인해 주세요."
    is ApiException.Http -> when (code) {
        "PURCHASE_ACCESS_DENIED" -> "구매한 계정과 현재 계정이 다릅니다. 구매한 계정으로 로그인해 주세요."
        "WITHDRAWN_MEMBER" -> "탈퇴한 계정으로는 구매할 수 없습니다."
        "PURCHASE_CANCELLED" -> "취소되었거나 환불된 구매입니다."
        "PURCHASE_QUANTITY_UNSUPPORTED" -> "한 번에 여러 개 구매는 지원하지 않습니다."
        "PAYMENT_PRODUCT_UNAVAILABLE" -> "현재 판매 중인 상품이 아닙니다."
        "PAYMENT_STORE_UNAVAILABLE" -> "Google Play 확인이 지연되고 있습니다. 잠시 후 다시 확인해 주세요."
        "INVALID_INPUT", "PURCHASE_INVALID", "PURCHASE_PRODUCT_MISMATCH" -> "구매 정보를 확인할 수 없습니다. ($code)"
        else -> "구매 요청에 실패했습니다. 잠시 후 다시 확인해 주세요."
    }
    else -> "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요."
}

fun BillingPurchaseOutcome.Error.paymentMessage(): String {
    val operation = when (source) {
        BillingErrorSource.PURCHASE_UPDATE -> "Google Play 결제 실패"
        BillingErrorSource.RESTORE_QUERY -> "Google Play 미처리 구매 조회 실패"
    }
    val detail = message.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
    return "$operation (코드: $responseCode)$detail"
}
