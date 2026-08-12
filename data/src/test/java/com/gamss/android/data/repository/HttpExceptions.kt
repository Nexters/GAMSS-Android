package com.gamss.android.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

internal fun httpException(statusCode: Int): HttpException {
    val errorBody = "{}".toResponseBody("application/json".toMediaType())
    return HttpException(Response.error<Unit>(statusCode, errorBody))
}
