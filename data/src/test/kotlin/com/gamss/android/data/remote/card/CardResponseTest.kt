package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.card.model.response.toDomain
import com.gamss.android.data.remote.card.model.response.toDomainOrNull
import com.gamss.android.domain.emotion.EmotionCharacter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CardResponseTest {

    @Test
    fun `card response preserves the card fields returned by the server`() {
        val card = CardResponse(
            id = 10L,
            conversationId = 20L,
            emotion = "ANGER",
            emotionLabel = "분노",
            summary = "비 때문에 하루가 꼬였어요",
            message = "비 때문에 하루가 꼬였어요",
            date = "2026-08-15",
        ).toDomain(requestedCharacter = EmotionCharacter.ANGER, fallbackDate = LocalDate.of(2026, 1, 1))

        assertEquals(10L, card.id)
        assertEquals(20L, card.conversationId)
        assertEquals(EmotionCharacter.ANGER, card.character)
        assertEquals("분노", card.emotionLabel)
        assertEquals("비 때문에 하루가 꼬였어요", card.summary)
        assertEquals(LocalDate.of(2026, 8, 15), card.date)
    }

    @Test
    fun `card creation falls back to the requested values when the response cannot be mapped`() {
        val fallbackDate = LocalDate.of(2026, 8, 17)

        val card = CardResponse(
            id = 10L,
            conversationId = 20L,
            emotion = "FUTURE_EMOTION",
            emotionLabel = "미래 감정",
            summary = "요약",
            message = "메시지",
            date = "invalid-date",
        ).toDomain(requestedCharacter = EmotionCharacter.ANGER, fallbackDate = fallbackDate)

        assertEquals(10L, card.id)
        assertEquals(EmotionCharacter.ANGER, card.character)
        assertEquals(fallbackDate, card.date)
    }

    @Test
    fun `invalid card responses do not hide valid cards in a list`() {
        val cards = listOf(
            CardResponse(
                id = 10L,
                conversationId = 20L,
                emotion = "ANGER",
                emotionLabel = "분노",
                summary = "요약",
                message = "메시지",
                date = "2026-08-15",
            ),
            CardResponse(
                id = 11L,
                conversationId = 20L,
                emotion = "FUTURE_EMOTION",
                emotionLabel = "미래 감정",
                summary = "요약",
                message = "메시지",
                date = "2026-08-15",
            ),
            CardResponse(
                id = 12L,
                conversationId = 20L,
                emotion = "ANGER",
                emotionLabel = "분노",
                summary = "요약",
                message = "메시지",
                date = "invalid-date",
            ),
        ).mapNotNull { it.toDomainOrNull() }

        assertEquals(listOf(10L), cards.map { it.id })
    }
}
