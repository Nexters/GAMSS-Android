package com.gamss.android.domain.emotion

import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionCharacterTest {

    @Test
    fun 모델_6감정을_대표_캐릭터로_1대1_매핑한다() {
        assertEquals(EmotionCharacter.JOY, EmotionCharacter.fromEmotionLabel(EmotionLabel.JOY))
        assertEquals(EmotionCharacter.ANGER, EmotionCharacter.fromEmotionLabel(EmotionLabel.ANGER))
        assertEquals(EmotionCharacter.ANXIETY, EmotionCharacter.fromEmotionLabel(EmotionLabel.ANXIETY))
        assertEquals(EmotionCharacter.QUIRKY, EmotionCharacter.fromEmotionLabel(EmotionLabel.EMBARRASSED))
        assertEquals(EmotionCharacter.PRICKLY, EmotionCharacter.fromEmotionLabel(EmotionLabel.HURT))
        assertEquals(EmotionCharacter.WARM, EmotionCharacter.fromEmotionLabel(EmotionLabel.SADNESS))
    }

    @Test
    fun 캐릭터_6종이_남김없이_하나씩_대응된다() {
        val mapped = EmotionLabel.entries.map { EmotionCharacter.fromEmotionLabel(it) }
        assertEquals(EmotionCharacter.entries.toSet(), mapped.toSet())
        assertEquals(EmotionCharacter.entries.size, mapped.distinct().size)
    }
}
