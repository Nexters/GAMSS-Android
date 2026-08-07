package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Conversation

data class ChattingListState(
    val conversations: List<Conversation> = emptyList(),
    // 첫 로드가 끝나기 전에 "대화가 없어요"가 깜빡이지 않도록 로딩으로 시작한다.
    val isLoading: Boolean = true,
)
