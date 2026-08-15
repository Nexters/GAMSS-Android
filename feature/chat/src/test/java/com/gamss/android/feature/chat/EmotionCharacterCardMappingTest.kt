package com.gamss.android.feature.chat

import com.gamss.android.core.designsystem.card.GamssEmotionCardCharacter
import com.gamss.android.domain.emotion.EmotionCharacter
import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionCharacterCardMappingTest {

    @Test
    fun `all emotion characters map to their card characters`() {
        val expectedCharacters = mapOf(
            EmotionCharacter.JOY to GamssEmotionCardCharacter.JOY,
            EmotionCharacter.ANGER to GamssEmotionCardCharacter.ANGER,
            EmotionCharacter.ANXIETY to GamssEmotionCardCharacter.ANXIETY,
            EmotionCharacter.SADNESS to GamssEmotionCardCharacter.SADNESS,
            EmotionCharacter.QUIRKY to GamssEmotionCardCharacter.QUIRKY,
            EmotionCharacter.PRICKLY to GamssEmotionCardCharacter.PRICKLY,
        )

        assertEquals(
            expectedCharacters,
            EmotionCharacter.entries.associateWith(EmotionCharacter::toGamssEmotionCardCharacter),
        )
    }
}
