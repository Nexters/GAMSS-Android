package com.gamss.android.data.remote.push.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterDeviceTokenRequest(
    val token: String,
)
