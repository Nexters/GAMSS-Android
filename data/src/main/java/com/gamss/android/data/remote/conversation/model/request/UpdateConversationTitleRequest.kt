package com.gamss.android.data.remote.conversation.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateConversationTitleRequest(
    val title: String,
)
