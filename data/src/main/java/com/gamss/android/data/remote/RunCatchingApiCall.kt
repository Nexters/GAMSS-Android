package com.gamss.android.data.remote

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.domain.auth.SessionExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * API 호출 결과를 [AppResult]로 변환한다. 인증이 필요한 API의 401은 세션 만료로 보고,
 * 로그인처럼 세션 없이 호출하는 API는 호출부에서 예외 처리한다.
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> runCatchingApiCall(
    treatUnauthorizedAsSessionExpired: Boolean = true,
    block: () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SessionExpiredException) {
        AppResult.Failure(e)
    } catch (e: HttpException) {
        val serverError = e.parseServerError()
        AppResult.Failure(
            if (treatUnauthorizedAsSessionExpired && e.code() == HTTP_UNAUTHORIZED) {
                SessionExpiredException(e)
            } else {
                ApiException.Http(
                    httpStatus = e.code(),
                    code = serverError?.code,
                    message = serverError?.message ?: e.message(),
                    cause = e,
                )
            },
        )
    } catch (e: IOException) {
        AppResult.Failure(ApiException.Network(e))
    } catch (e: Throwable) {
        AppResult.Failure(e)
    }

/**
 * 서버는 실패를 항상 실제 HTTP 상태 코드(4xx/5xx)로 내려주면서, 바디에는
 * `{success:false, error:{code, message}}` envelope를 함께 싣는다. Retrofit은 2xx가
 * 아니면 바디를 응답 타입으로 변환하기 전에 [HttpException]부터 던지므로, 그 안의
 * errorBody를 직접 파싱하지 않으면 `code`/`message`가 유실되고 Retrofit의 일반 문구
 * (예: "HTTP 500 Internal Server Error")만 남는다.
 */
@Suppress("SwallowedException", "TooGenericExceptionCaught")
private fun HttpException.parseServerError(): ApiError? =
    try {
        response()?.errorBody()?.string()
            ?.let { errorBodyJson.decodeFromString<ServerErrorEnvelope>(it) }
            ?.error
    } catch (e: Exception) {
        null
    }

@Serializable
private data class ServerErrorEnvelope(
    val error: ApiError? = null,
)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

private const val HTTP_UNAUTHORIZED = 401
