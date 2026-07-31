package com.gamss.android.data.remote.emotion

import com.gamss.android.domain.emotion.EmotionCharacter
import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionTypeMappingTest {

    @Test
    fun 캐릭터_여섯_종이_서버_문자열과_왕복한다() {
        EmotionCharacter.entries.forEach { character ->
            assertEquals(character, character.toServerEmotionType().toEmotionCharacter())
        }
    }

    @Test
    fun 이름이_다른_까칠은_GRUMPY_로_보낸다() {
        assertEquals("GRUMPY", EmotionCharacter.PRICKLY.toServerEmotionType())
    }
}
