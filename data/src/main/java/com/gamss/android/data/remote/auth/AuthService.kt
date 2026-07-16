package com.gamss.android.data.remote.auth

import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}