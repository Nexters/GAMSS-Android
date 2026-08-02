package com.gamss.android.core.common.network

sealed class ApiException(
    message: String,
    val httpStatus: Int? = null,
    val code: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    class Http(
        httpStatus: Int? = null,
        code: String? = null,
        message: String,
        cause: Throwable? = null,
    ) : ApiException(message, httpStatus, code, cause)

    class Network(cause: Throwable) : ApiException("Network request failed", cause = cause)
}
