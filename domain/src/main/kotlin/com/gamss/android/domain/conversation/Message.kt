package com.gamss.android.domain.conversation

import com.gamss.android.domain.emotion.EmotionCharacter

/** 화면이 쓰지 않는 서버 필드(작성 시각, rootMessageId)는 담지 않는다. */
data class Message(
    val id: Long,
    val conversationId: Long,
    val sender: MessageSender,
    val content: String,
    val repliesToMessageId: Long? = null,
)

sealed interface MessageSender {
    data object User : MessageSender
    data class Character(val character: EmotionCharacter) : MessageSender
}
