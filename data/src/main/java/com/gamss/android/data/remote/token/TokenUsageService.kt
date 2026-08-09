package com.gamss.android.data.remote.token

import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.token.model.response.DailyTokenUsageDataResponse
import retrofit2.http.GET

internal interface TokenUsageService {

    @GET("/api/members/me/token-usage")
    suspend fun getDailyTokenUsage(): ApiResponse<DailyTokenUsageDataResponse>
}
