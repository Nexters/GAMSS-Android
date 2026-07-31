package com.gamss.android.data.remote.card.model.request

import kotlinx.serialization.Serializable

/** [emotion] 은 모델 6감정이 아니라 캐릭터 6종 문자열이다. */
@Serializable
internal data class CreateCardRequest(
    val conversationId: Long,
    val emotion: String,
    val summary: String,
)
