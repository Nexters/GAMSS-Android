package com.gamss.android.domain.payment

/**
 * 성공 여부와 분기에 필요한 응답 종류는 Play 어댑터(core:billing) 안에서 이미 판정해서 담아준다 —
 * 호출부는 [isSuccess]·[status]만 보고 분기하면 되고, SDK 응답 코드 상수를 몰라도 된다.
 * [responseCode]는 해석하지 않고 로그 등 참고용으로만 쓴다.
 */
data class BillingOperationResult(
    val isSuccess: Boolean,
    val responseCode: Int,
    val status: BillingResultStatus = if (isSuccess) BillingResultStatus.OK else BillingResultStatus.ERROR,
)

/** 호출부가 다르게 처리해야 하는 응답만 구분한다. 나머지 실패는 모두 [ERROR]다. */
enum class BillingResultStatus {
    OK,
    USER_CANCELED,
    ITEM_ALREADY_OWNED,
    ERROR,
}
