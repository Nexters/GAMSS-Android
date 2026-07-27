package com.gamss.android.data.auth

import com.gamss.android.data.local.auth.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class TokenInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenProvider.getAccessToken()
        val request = if (accessToken.isNullOrBlank()) {
            originalRequest
        } else {
            originalRequest.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
                .build()
        }

        return chain.proceed(request)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
