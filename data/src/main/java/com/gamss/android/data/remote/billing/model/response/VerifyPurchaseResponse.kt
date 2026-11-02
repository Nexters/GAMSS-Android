package com.gamss.android.data.remote.billing.model.response

import com.gamss.android.domain.billing.GrantResult
import kotlinx.serialization.Serializable

@Serializable
internal data class VerifyPurchaseResponse(
    val alreadyGranted: Boolean = false,
)

internal fun VerifyPurchaseResponse.toDomain(): GrantResult =
    if (alreadyGranted) GrantResult.AlreadyGranted else GrantResult.Granted
