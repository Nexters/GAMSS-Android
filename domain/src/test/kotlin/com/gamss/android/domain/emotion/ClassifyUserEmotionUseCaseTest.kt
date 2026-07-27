package com.gamss.android.domain.emotion

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Suppress("MagicNumber")
class ClassifyUserEmotionUseCaseTest {

    /** 발화 텍스트 → 점수 분포로 응답하는 fake. 집계는 scores 만 쓰므로 topLabel/conf 는 무의미값. */
    private fun classifierOf(responses: Map<String, Map<String, Float>>) =
        object : EmotionClassifier {
            override suspend fun classify(text: String): ClassificationResult {
                val scores = responses[text] ?: mapOf("중립" to 1.0f)
                return ClassificationResult(topLabel = "", confidence = 0f, scores = scores)
            }
        }

    @Test
    fun 발화별_점수합산으로_지배적_감정을_고른다() = runBlocking {
        val useCase = ClassifyUserEmotionUseCase(
            classifierOf(
                mapOf(
                    "오늘 너무 힘들었어" to mapOf("슬픔" to 0.6f, "분노" to 0.4f),
                    "자꾸 눈물이 나" to mapOf("슬픔" to 0.55f, "분노" to 0.45f),
                ),
            ),
        )
        val result = useCase(listOf("오늘 너무 힘들었어", "자꾸 눈물이 나"))
        assertEquals(EmotionLabel.SADNESS, result?.label)
        assertEquals(EmotionCharacter.WARM, result?.character)
    }

    @Test
    fun 한_발화가_튀어도_합산이_이긴다() = runBlocking {
        // 분노 합 0.95+0.1+0.1=1.15 vs 슬픔 합 0.05+0.9+0.9=1.85 → 슬픔
        val useCase = ClassifyUserEmotionUseCase(
            classifierOf(
                mapOf(
                    "진짜 화나!" to mapOf("분노" to 0.95f, "슬픔" to 0.05f),
                    "그래도 너무 슬퍼" to mapOf("분노" to 0.1f, "슬픔" to 0.9f),
                    "계속 눈물나" to mapOf("분노" to 0.1f, "슬픔" to 0.9f),
                ),
            ),
        )
        val result = useCase(listOf("진짜 화나!", "그래도 너무 슬퍼", "계속 눈물나"))
        assertEquals(EmotionLabel.SADNESS, result?.label)
    }

    @Test
    fun 단일_발화를_분류한다() = runBlocking {
        val useCase = ClassifyUserEmotionUseCase(
            classifierOf(mapOf("합격했어 너무 기뻐" to mapOf("기쁨" to 0.99f, "슬픔" to 0.01f))),
        )
        val result = useCase(listOf("합격했어 너무 기뻐"))
        assertEquals(EmotionLabel.JOY, result?.label)
        assertEquals(EmotionCharacter.JOY, result?.character)
    }

    @Test
    fun 빈_목록이나_공백은_null() = runBlocking {
        val useCase = ClassifyUserEmotionUseCase(classifierOf(emptyMap()))
        assertNull(useCase(emptyList()))
        assertNull(useCase(listOf("   ", "")))
    }
}
