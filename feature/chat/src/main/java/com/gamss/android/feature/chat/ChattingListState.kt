package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Conversation

data class ChattingListState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
)
