package com.gamss.android.data.remote

import kotlinx.serialization.json.Json

/** encodeDefaults 를 켜면 SaveMessageRequest 가 conversationId=null 을 보내 새 채팅방 생성이 깨진다. */
internal val gamssJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = false
}
