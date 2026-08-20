package com.gamss.android.domain.conversation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PendingConversationRevealTest {

    @Test
    fun 저장한_값을_같은_대화_ID로_꺼낼_수_있다() = runBlocking {
        val reveal = PendingConversationReveal()
        val sent = sentMessage()

        reveal.save(sent, now = BASE_TIME)

        assertEquals(sent, reveal.consume(ROOM_ID, now = BASE_TIME)?.sent)
    }

    @Test
    fun 저장_시각이_생성_일시로_즉시_채워진다() = runBlocking {
        val reveal = PendingConversationReveal()
        reveal.save(sentMessage(), now = BASE_TIME)

        val result = reveal.consume(ROOM_ID, now = BASE_TIME)

        assertEquals(
            Instant.ofEpochMilli(BASE_TIME).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            result?.createdAt,
        )
    }

    @Test
    fun 한_번_꺼내면_다시는_꺼낼_수_없다() = runBlocking {
        val reveal = PendingConversationReveal()
        reveal.save(sentMessage(), now = BASE_TIME)

        reveal.consume(ROOM_ID, now = BASE_TIME)

        assertNull(reveal.consume(ROOM_ID, now = BASE_TIME))
    }

    @Test
    fun 다른_대화_ID로는_꺼낼_수_없고_원래_값은_남는다() = runBlocking {
        val reveal = PendingConversationReveal()
        val sent = sentMessage()
        reveal.save(sent, now = BASE_TIME)

        assertNull(reveal.consume(OTHER_ROOM_ID, now = BASE_TIME))
        assertEquals(sent, reveal.consume(ROOM_ID, now = BASE_TIME)?.sent)
    }

    @Test
    fun 만료_시간_이내면_꺼낼_수_있다() = runBlocking {
        val reveal = PendingConversationReveal()
        reveal.save(sentMessage(), now = BASE_TIME)

        val result = reveal.consume(ROOM_ID, now = BASE_TIME + PENDING_REVEAL_EXPIRY_MILLIS)

        assertEquals(ROOM_ID, result?.sent?.message?.conversationId)
    }

    @Test
    fun 만료_시간이_지나면_꺼내지_못하고_폐기된다() = runBlocking {
        val reveal = PendingConversationReveal()
        reveal.save(sentMessage(), now = BASE_TIME)

        val expired = reveal.consume(ROOM_ID, now = BASE_TIME + PENDING_REVEAL_EXPIRY_MILLIS + 1)

        assertNull(expired)
        // 만료돼 버려졌으니 시간을 되돌려도 다시 꺼낼 수 없다 — 자리는 이미 비었다.
        assertNull(reveal.consume(ROOM_ID, now = BASE_TIME))
    }

    private fun sentMessage() = SentMessage(
        message = Message(
            id = MESSAGE_ID,
            conversationId = ROOM_ID,
            sender = MessageSender.User,
            content = "아 진짜 짜증나",
        ),
        commentStatus = CommentGenerationStatus.DONE,
        comments = emptyList(),
    )

    private companion object {
        const val ROOM_ID = 7L
        const val OTHER_ROOM_ID = 8L
        const val MESSAGE_ID = 100L
        const val BASE_TIME = 1_700_000_000_000L
    }
}
