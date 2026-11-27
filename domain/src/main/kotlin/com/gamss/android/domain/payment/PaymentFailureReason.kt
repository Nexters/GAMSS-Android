package com.gamss.android.domain.payment

import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.auth.SessionExpiredException

/**
 * 결제 실패 원인을 설명하는 진단용 문구. [PaymentState.message]와 상점 화면의 진단 정보처럼 디버그 화면에서만
 * 쓴다. 사용자에게 보여주는 안내는 [PaymentFailureReason]을 기준으로 feature의 string 리소스로 만든다.
 */
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

/**
 * 서버 구매 검증 실패를 사용자 안내가 달라지는 단위로 묶는다. [RETRYABLE]만 다시 시도로 해결될 수
 * 있고, 나머지는 다시 시도해도 같은 결과라 원인을 그대로 안내해야 한다.
 */
enum class PaymentFailureReason {
    RETRYABLE,
    LOGIN_REQUIRED,
    ACCOUNT_MISMATCH,
    WITHDRAWN_MEMBER,
    PURCHASE_CANCELLED,
    PRODUCT_UNAVAILABLE,
    INVALID_PURCHASE,
}

fun Throwable.paymentFailureReason(): PaymentFailureReason = when (this) {
    is SessionExpiredException -> PaymentFailureReason.LOGIN_REQUIRED
    is ApiException.Http -> when (code) {
        "PURCHASE_ACCESS_DENIED" -> PaymentFailureReason.ACCOUNT_MISMATCH
        "WITHDRAWN_MEMBER" -> PaymentFailureReason.WITHDRAWN_MEMBER
        "PURCHASE_CANCELLED" -> PaymentFailureReason.PURCHASE_CANCELLED
        "PAYMENT_PRODUCT_UNAVAILABLE" -> PaymentFailureReason.PRODUCT_UNAVAILABLE
        "INVALID_INPUT",
        "PURCHASE_INVALID",
        "PURCHASE_PRODUCT_MISMATCH",
        "PURCHASE_QUANTITY_UNSUPPORTED",
        -> PaymentFailureReason.INVALID_PURCHASE
        else -> PaymentFailureReason.RETRYABLE
    }
    else -> PaymentFailureReason.RETRYABLE
}
