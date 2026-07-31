package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.userUtterances
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationMessageTest {

    private fun msg(id: Long, sender: String, content: String) = ConversationMessage(
        id = id,
        conversationId = 1,
        senderType = sender,
        emotionType = null,
        content = content,
        repliesToMessageId = null,
        rootMessageId = null,
        createdAt = "2026-07-23T06:54:32Z",
    )

    @Test
    fun USER_발화만_순서대로_추출하고_캐릭터는_제외한다() {
        val messages = listOf(
            msg(1, "USER", "오늘 억울한 일이 있었어"),
            msg(2, "CHARACTER", "세상에 억울한 일 한두 개냐, 그냥 넘겨."),
            msg(3, "CHARACTER", "무슨 일인데?? 더 크게 번지는 거 아니야??"),
            msg(11, "USER", "그러게 그냥 맛있는거 먹고 쉬려고"),
        )
        assertEquals(
            listOf("오늘 억울한 일이 있었어", "그러게 그냥 맛있는거 먹고 쉬려고"),
            messages.userUtterances(),
        )
    }
}
