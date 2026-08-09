package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.toDomain
import com.gamss.android.data.remote.conversation.model.response.userUtterances
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationMessageTest {

    private fun msg(
        id: Long,
        sender: String,
        content: String,
        emotionType: String? = null,
    ) = ConversationMessage(
        id = id,
        conversationId = 1,
        senderType = sender,
        emotionType = emotionType,
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

    @Test
    fun USER_메시지는_사용자_발신으로_옮긴다() {
        val domain = msg(1, "USER", "오늘 억울한 일이 있었어").toDomain()

        assertEquals(MessageSender.User, domain?.sender)
        assertEquals("오늘 억울한 일이 있었어", domain?.content)
    }

    @Test
    fun 서버_emotionType_여섯_종을_캐릭터로_옮긴다() {
        val expected = mapOf(
            "JOY" to EmotionCharacter.JOY,
            "ANGER" to EmotionCharacter.ANGER,
            "ANXIETY" to EmotionCharacter.ANXIETY,
            "GRUMPY" to EmotionCharacter.PRICKLY,
            "WARM" to EmotionCharacter.WARM,
            "QUIRKY" to EmotionCharacter.QUIRKY,
        )

        expected.forEach { (emotionType, character) ->
            val domain = msg(1, "CHARACTER", "그래서 어쩌라고", emotionType = emotionType).toDomain()

            assertEquals(MessageSender.Character(character), domain?.sender)
        }
    }

    @Test
    fun 미확인_emotionType도_메시지는_보존한다() {
        val domain = msg(1, "CHARACTER", "정체불명 답장", emotionType = "MYSTERY").toDomain()

        assertEquals(MessageSender.Unknown, domain.sender)
        assertEquals("정체불명 답장", domain.content)
    }

    @Test
    fun 미확인_senderType도_메시지는_보존한다() {
        val domain = msg(1, "SYSTEM", "시스템 메시지").toDomain()

        assertEquals(MessageSender.Unknown, domain.sender)
        assertEquals("시스템 메시지", domain.content)
    }
}
