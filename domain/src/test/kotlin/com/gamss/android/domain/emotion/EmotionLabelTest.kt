package com.gamss.android.domain.emotion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EmotionLabelTest {

    @Test
    fun 한글_라벨을_대응하는_감정으로_해석한다() {
        assertEquals(EmotionLabel.JOY, EmotionLabel.fromKoLabel("기쁨"))
        assertEquals(EmotionLabel.SADNESS, EmotionLabel.fromKoLabel("슬픔"))
    }

    @Test
    fun 알_수_없는_라벨은_예외() {
        assertThrows(IllegalStateException::class.java) {
            EmotionLabel.fromKoLabel("중립")
        }
    }
}
