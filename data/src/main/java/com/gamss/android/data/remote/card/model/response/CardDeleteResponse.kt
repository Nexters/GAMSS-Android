package com.gamss.android.data.remote.card.model.response

import kotlinx.serialization.Serializable

@Serializable
internal data class CardDeleteResponse(
    val deletedCount: Int? = null,
)
