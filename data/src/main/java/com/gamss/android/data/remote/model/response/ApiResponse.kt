package com.gamss.android.data.remote.model.response

import kotlinx.serialization.Serializable

@Serializable
internal data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
)

@Serializable
internal data class ApiError(
    val code: String? = null,
    val message: String? = null,
)
