package com.gamss.android.data.auth

import com.gamss.android.data.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * TODO(google-login 머지 시 삭제): 저장된 토큰을 쓰는 TokenInterceptor 로 대체한다.
 * 이 브랜치에 로그인 흐름이 없어 debug 빌드에서만 임시로 토큰을 붙인다.
 */
internal class DevTokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (BuildConfig.DEV_ACCESS_TOKEN.isBlank()) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX${BuildConfig.DEV_ACCESS_TOKEN}")
                .build(),
        )
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
