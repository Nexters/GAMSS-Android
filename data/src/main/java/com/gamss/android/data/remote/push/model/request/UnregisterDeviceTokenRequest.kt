package com.gamss.android.data.remote.push.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class UnregisterDeviceTokenRequest(
    val token: String,
)
