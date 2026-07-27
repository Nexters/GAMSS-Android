package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.model.SessionExpiredException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * API 호출 결과를 [AppResult]로 변환한다. 401/403은 어느 API에서 발생했든 세션이 깨졌다는
 * 뜻이므로 전역적으로 [SessionExpiredException]으로 통일하고, 그 외 실패는 전송 계층 형태
 * ([ApiException])로만 구분한다. 특정 기능에서만 의미가 달라지는 실패는
 * 각 리포지토리 호출부에서 별도로 해석한다.
 *
 * @param treatUnauthorizedAsSessionExpired 로그인처럼 애초에 세션(Authorization 헤더)이
 * 없는 상태로 호출하는 API는 401/403이 "자격 증명 거부"일 뿐 세션 만료가 아니므로 false로 둔다.
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> runCatchingApiCall(
    treatUnauthorizedAsSessionExpired: Boolean = true,
    block: () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: SessionExpiredException) {
        AppResult.Failure(e)
    } catch (e: HttpException) {
        val serverError = e.parseServerError()
        AppResult.Failure(
            if (treatUnauthorizedAsSessionExpired &&
                (e.code() == HTTP_UNAUTHORIZED || e.code() == HTTP_FORBIDDEN)
            ) {
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
internal fun HttpException.parseServerError(): ServerErrorBody? =
    try {
        response()?.errorBody()?.string()
            ?.let { errorBodyJson.decodeFromString<ServerErrorEnvelope>(it) }
            ?.error
    } catch (e: Exception) {
        null
    }

@Serializable
internal data class ServerErrorEnvelope(
    val error: ServerErrorBody? = null,
)

@Serializable
internal data class ServerErrorBody(
    val code: String? = null,
    val message: String? = null,
)

internal val errorBodyJson = Json { ignoreUnknownKeys = true }

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
