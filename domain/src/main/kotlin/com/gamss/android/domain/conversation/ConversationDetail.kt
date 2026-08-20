package com.gamss.android.domain.conversation

data class ConversationDetail(
    val conversation: Conversation,
    val messages: List<Message>,
)
