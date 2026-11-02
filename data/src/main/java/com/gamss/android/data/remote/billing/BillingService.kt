package com.gamss.android.data.remote.billing

import com.gamss.android.data.remote.billing.model.request.VerifyPurchaseRequest
import com.gamss.android.data.remote.billing.model.response.BillingProductResponse
import com.gamss.android.data.remote.billing.model.response.VerifyPurchaseResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface BillingService {

    @GET("/api/billing/products")
    suspend fun getProducts(): ApiResponse<List<BillingProductResponse>>

    /** 서버가 purchaseToken 을 Play 서버와 대조해 검증하고 지급까지 확정한다. */
    @POST("/api/billing/purchases/verify")
    suspend fun verifyPurchase(@Body request: VerifyPurchaseRequest): ApiResponse<VerifyPurchaseResponse>
}
