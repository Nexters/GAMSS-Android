package com.gamss.android.data.network

import com.gamss.android.data.local.auth.TokenProvider
import com.gamss.android.data.remote.auth.LOGIN_PATH
import com.gamss.android.data.remote.auth.REISSUE_TOKENS_PATH
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private val NO_AUTH_HEADER_PATHS = setOf(LOGIN_PATH, REISSUE_TOKENS_PATH)

class TokenInterceptor @Inject constructor(
   private val tokenProvider: TokenProvider
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.url.encodedPath in NO_AUTH_HEADER_PATHS) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenProvider.getAccessToken()

        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}