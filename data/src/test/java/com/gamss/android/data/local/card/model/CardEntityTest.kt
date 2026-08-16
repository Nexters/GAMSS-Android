package com.gamss.android.data.local.card.model

import com.gamss.android.domain.emotion.EmotionCharacter
import org.junit.Assert.assertEquals
import org.junit.Test

class CardEntityTest {

    @Test
    fun `Room에 캐시한 감정 키는 여섯 종류 이미지 캐릭터로 복원된다`() {
        val expectedCharacters = mapOf(
            "JOY" to EmotionCharacter.JOY,
            "ANGER" to EmotionCharacter.ANGER,
            "ANXIETY" to EmotionCharacter.ANXIETY,
            "SADNESS" to EmotionCharacter.SADNESS,
            "QUIRKY" to EmotionCharacter.QUIRKY,
            "GRUMPY" to EmotionCharacter.PRICKLY,
        )

        val restoredCharacters = expectedCharacters.keys.associateWith { emotion ->
            cardEntity(emotion).toDomain().character
        }

        assertEquals(expectedCharacters, restoredCharacters)
    }

    @Test(expected = IllegalStateException::class)
    fun `알 수 없는 감정 키는 잘못된 이미지로 대체하지 않는다`() {
        cardEntity(emotion = "UNKNOWN").toDomain()
    }

    private fun cardEntity(emotion: String) = CardEntity(
        id = 10L,
        conversationId = 7L,
        emotion = emotion,
        emotionLabel = "감정",
        summary = "요약",
        message = "메시지",
        date = "2026-08-16",
    )
}
