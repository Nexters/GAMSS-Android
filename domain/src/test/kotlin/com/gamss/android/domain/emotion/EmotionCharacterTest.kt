package com.gamss.android.domain.emotion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EmotionCharacterTest {

    @Test
    fun 모델_6감정을_대표_캐릭터로_1대1_매핑한다() {
        assertEquals(EmotionCharacter.JOY, EmotionCharacter.fromEmotionLabel("기쁨"))
        assertEquals(EmotionCharacter.ANGER, EmotionCharacter.fromEmotionLabel("분노"))
        assertEquals(EmotionCharacter.ANXIETY, EmotionCharacter.fromEmotionLabel("불안"))
        assertEquals(EmotionCharacter.QUIRKY, EmotionCharacter.fromEmotionLabel("당황"))
        assertEquals(EmotionCharacter.PRICKLY, EmotionCharacter.fromEmotionLabel("상처"))
        assertEquals(EmotionCharacter.WARM, EmotionCharacter.fromEmotionLabel("슬픔"))
    }

    @Test
    fun 알_수_없는_라벨은_예외() {
        assertThrows(IllegalStateException::class.java) {
            EmotionCharacter.fromEmotionLabel("중립")
        }
    }
}
