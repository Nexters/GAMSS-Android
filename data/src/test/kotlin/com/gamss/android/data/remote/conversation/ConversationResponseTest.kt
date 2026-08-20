package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.response.ConversationDetailResponse
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.conversation.model.response.parseConversationCreatedAt
import com.gamss.android.data.remote.conversation.model.response.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ConversationResponseTest {

    @Test
    fun 아직_제목이_없는_방은_null_그대로_전달한다() {
        val conversation = ConversationResponse(id = 37, title = null).toDomain()

        assertEquals(37L, conversation?.id)
        assertNull(conversation?.title)
    }

    @Test
    fun 방_id_가_없는_항목은_버린다() {
        assertNull(ConversationResponse(id = null, title = "팀장이 아이디어 가로챘어").toDomain())
    }

    @Test
    fun 시각을_주지_않으면_생성_시각도_null_이다() {
        val conversation = ConversationResponse(id = 37, title = "제목").toDomain()

        assertNull(conversation?.createdAt)
    }

    @Test
    fun 오프셋이_없는_시각은_그대로_읽는다() {
        val parsed = parseConversationCreatedAt("2026-07-31T04:20:00")

        assertEquals(LocalDateTime.of(2026, 7, 31, 4, 20), parsed)
    }

    @Test
    fun 오프셋이_붙은_시각은_주어진_존으로_옮긴다() {
        val parsed = parseConversationCreatedAt(
            raw = "2026-07-30T19:20:00Z",
            zone = ZoneId.of("Asia/Seoul"),
        )

        assertEquals(LocalDateTime.of(2026, 7, 31, 4, 20), parsed)
    }

    @Test
    fun 정상적인_시각은_도메인까지_전달된다() {
        val conversation = ConversationResponse(
            id = 37,
            title = "제목",
            createdAt = "2026-07-31T04:20:00",
        ).toDomain()

        assertEquals(LocalDateTime.of(2026, 7, 31, 4, 20), conversation?.createdAt)
    }

    @Test
    fun 형식이_어긋난_시각은_null_이지만_항목은_살아남는다() {
        val conversation = ConversationResponse(
            id = 37,
            title = "제목",
            createdAt = "31/07/2026 04:20",
        ).toDomain()

        assertEquals(37L, conversation?.id)
        assertNull(conversation?.createdAt)
    }

    @Test
    fun 빈_문자열_시각은_null_이다() {
        assertNull(parseConversationCreatedAt(" "))
    }

    @Test
    fun 대화_상세는_생성_시각과_메시지를_도메인으로_옮긴다() {
        val detail = ConversationDetailResponse(
            conversation = ConversationResponse(
                id = 37,
                title = "비 오는 날의 짜증",
                createdAt = "2026-08-15T17:16:52.320",
            ),
            messages = listOf(
                ConversationMessage(
                    id = 1,
                    conversationId = 37,
                    senderType = ConversationMessage.SENDER_USER,
                    content = "오늘 억울한 일이 있었어",
                    createdAt = "2026-08-15T17:16:52.320",
                ),
            ),
        ).toDomain()

        assertEquals(LocalDateTime.of(2026, 8, 15, 17, 16, 52, 320_000_000), detail?.conversation?.createdAt)
        assertEquals(listOf(1L), detail?.messages?.map { it.id })
    }
}
