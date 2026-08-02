package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.assertSuccess
import com.gamss.android.domain.emotion.ClassificationResult
import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.emotion.EmotionClassifier
import com.gamss.android.domain.emotion.EmotionLabel
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MagicNumber")
class GenerateCardInputUseCaseTest {

    private fun useCase(label: String, summary: String): GenerateCardInputUseCase {
        val classifier = object : EmotionClassifier {
            override suspend fun classify(text: String): ClassificationResult =
                ClassificationResult(label, 0.9f, mapOf(label to 0.9f))
        }
        val summarizer = object : DiarySummarizer {
            override suspend fun summarize(text: String): String = summary
        }
        return GenerateCardInputUseCase(
            ClassifyUserEmotionUseCase(classifier),
            SummarizeDiaryUseCase(summarizer),
        )
    }

    @Test
    fun 발화가_있으면_감정과_캐릭터와_요약을_담은_CardInput_반환() = runBlocking {
        val useCase = useCase(label = "슬픔", summary = "요약본")
        val result = useCase(listOf("강아지가 떠났어", "너무 슬퍼")).assertSuccess()
        assertEquals(EmotionLabel.SADNESS, result?.emotion)
        assertEquals(EmotionCharacter.WARM, result?.character)
        // 짧은 입력이라 요약은 게이팅되어 결합 원문이 그대로 요약이 된다.
        assertEquals("강아지가 떠났어 너무 슬퍼", result?.summary)
    }

    @Test
    fun USER_발화가_없으면_Success_null() = runBlocking {
        val useCase = useCase(label = "기쁨", summary = "x")
        assertNull(useCase(emptyList()).assertSuccess())
        assertNull(useCase(listOf("   ")).assertSuccess())
    }

    @Test
    fun 하위_UseCase_가_실패하면_Failure_로_전파된다() = runBlocking {
        val failing = object : EmotionClassifier {
            override suspend fun classify(text: String): ClassificationResult = error("추론 실패")
        }
        val summarizer = object : DiarySummarizer {
            override suspend fun summarize(text: String): String = "요약본"
        }
        val useCase = GenerateCardInputUseCase(
            ClassifyUserEmotionUseCase(failing),
            SummarizeDiaryUseCase(summarizer),
        )
        assertTrue(useCase(listOf("강아지가 떠났어")) is AppResult.Failure)
    }
}
