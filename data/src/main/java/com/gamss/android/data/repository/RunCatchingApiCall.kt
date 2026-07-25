package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.auth.model.response.AuthRequestException
import com.gamss.android.domain.model.AuthException
import retrofit2.HttpException
import java.io.IOException

/**
 * API 호출 결과를 [AppResult]로 변환한다. 인증이 필요한 API는 전부 같은 방식으로
 * 실패 원인(서버 거부/세션 만료/네트워크 오류)을 분류해야 하므로 레포지토리 간에 공유한다.
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> runCatchingApiCall(block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: AuthException) {
        AppResult.Failure(e)
    } catch (e: AuthRequestException) {
        AppResult.Failure(AuthException.InvalidCredentials(e.message, e))
    } catch (e: HttpException) {
        AppResult.Failure(
            if (e.code() == HTTP_UNAUTHORIZED || e.code() == HTTP_FORBIDDEN) {
                AuthException.SessionExpired(e)
            } else {
                AuthException.Network(e)
            },
        )
    } catch (e: IOException) {
        AppResult.Failure(AuthException.Network(e))
    } catch (e: Throwable) {
        AppResult.Failure(e)
    }

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
