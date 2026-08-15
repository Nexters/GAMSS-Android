package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.card.model.response.toDomain
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
        ).toDomain()

        assertEquals(10L, card.id)
        assertEquals(20L, card.conversationId)
        assertEquals(EmotionCharacter.ANGER, card.character)
        assertEquals("분노", card.emotionLabel)
        assertEquals("비 때문에 하루가 꼬였어요", card.summary)
        assertEquals(LocalDate.of(2026, 8, 15), card.date)
    }
}
