package com.gamss.android.domain.conversation

import com.gamss.android.domain.emotion.EmotionCharacter

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
    data object Unknown : MessageSender
}
