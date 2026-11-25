package com.gamss.android.domain.payment

import com.gamss.android.core.common.AppResult

interface PaymentRepository {

    /** 지급량 오름차순. 가격은 내려주지 않으며, 각 스토어 SDK가 productId로 현지 통화 가격을 가져온다. */
    suspend fun getProducts(): AppResult<List<TokenProduct>>

    /**
     * 스토어 구매 시 계정 토큰(Google obfuscatedAccountId)으로 넘길 값.
     * 같은 회원에게는 항상 같고, 탈퇴 후 같은 소셜 계정으로 재가입해도 같다. 구매 직전마다 호출해도 안전하다.
     */
    suspend fun getAccountToken(): AppResult<String>

    /**
     * 서버가 Google에 직접 구매를 확인한 뒤 지급한다. 앱은 consumeAsync·acknowledgePurchase를
     * 부르지 않는다 — 서버가 지급한 뒤 consume한다.
     * 같은 구매를 다시 보내도 추가 지급 없이 같은 grantedTokens로 성공한다. 결제가 아직 끝나지
     * 않았으면 [PaymentStatus.PENDING]으로 응답하며 지급하지 않는다 — 지급 완료로 표시하지 말고
     * 결제가 끝난 뒤 다시 보내야 한다.
     */
    suspend fun verifyGooglePurchase(productId: String, purchaseToken: String): AppResult<PaymentGrant>
}
