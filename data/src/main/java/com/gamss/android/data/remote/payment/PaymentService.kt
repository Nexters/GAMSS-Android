package com.gamss.android.data.remote.payment

import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.payment.model.request.GooglePurchaseRequest
import com.gamss.android.data.remote.payment.model.response.AccountTokenResponse
import com.gamss.android.data.remote.payment.model.response.PaymentGrantResponse
import com.gamss.android.data.remote.payment.model.response.TokenProductResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface PaymentService {
    @GET("/api/payments/products")
    suspend fun getProducts(): ApiResponse<List<TokenProductResponse>>

    @GET("/api/payments/account-token")
    suspend fun getAccountToken(): ApiResponse<AccountTokenResponse>

    @POST("/api/payments/google")
    suspend fun verifyGooglePurchase(@Body request: GooglePurchaseRequest): ApiResponse<PaymentGrantResponse>
}
