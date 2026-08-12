package com.gamss.android.data.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.local.auth.TokenProvider
import com.gamss.android.domain.auth.AuthRepository
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * accessToken 만료로 인한 401 응답을 감지해 refreshToken으로 재발급을 시도하고,
 * 여러 요청이 동시에 401을 받아도 재발급은 한 번만 수행한다.
 */
internal class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val authRepository: Lazy<AuthRepository>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? =
        if (response.priorResponse != null) {
            null
        } else {
            synchronized(this) {
                retryWithReissuedToken(response)
            }
        }

    private fun retryWithReissuedToken(response: Response): Request? {
        val failedAccessToken = response.request.header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)
        val cachedAccessToken = tokenProvider.getAccessToken()

        return if (cachedAccessToken != null && cachedAccessToken != failedAccessToken) {
            // 다른 스레드가 이미 재발급을 완료했다면 그 토큰으로 재시도한다.
            response.request.withBearerToken(cachedAccessToken)
        } else {
            when (runBlocking { authRepository.get().reissueTokens() }) {
                is AppResult.Success -> {
                    tokenProvider.getAccessToken()?.let { accessToken ->
                        response.request.withBearerToken(accessToken)
                    }
                }

                is AppResult.Failure -> {
                    null
                }
            }
        }
    }

    private fun Request.withBearerToken(accessToken: String): Request =
        newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
            .build()
}
