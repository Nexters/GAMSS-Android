package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.card.model.response.toDomainOrNull
import com.gamss.android.data.remote.gamssJson
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardResponseTest {

    @Test
    fun `명세의 추가 필드가 있어도 카드 응답을 읽는다`() {
        val response = gamssJson.decodeFromString<CardResponse>(
            """
            {"id":1,"conversationId":2,"emotion":"ANGER","summary":"요약","message":"대사",
            "emotionLabel":"분노","date":"2026-08-15"}
            """.trimIndent(),
        )

        val card = response.toDomainOrNull()

        assertEquals(1L, card?.id)
        assertEquals(EmotionCharacter.ANGER, card?.character)
    }

    @Test
    fun `필수 필드가 누락된 응답은 카드로 변환하지 않는다`() {
        val response = gamssJson.decodeFromString<CardResponse>(
            """{"conversationId":2,"emotion":"ANGER","summary":"요약","message":"대사"}""",
        )

        assertNull(response.toDomainOrNull())
    }

    @Test
    fun `알 수 없는 감정은 카드로 변환하지 않는다`() {
        val response = gamssJson.decodeFromString<CardResponse>(
            """{"id":1,"conversationId":2,"emotion":"NEW_EMOTION","summary":"요약","message":"대사"}""",
        )

        assertNull(response.toDomainOrNull())
    }
}
