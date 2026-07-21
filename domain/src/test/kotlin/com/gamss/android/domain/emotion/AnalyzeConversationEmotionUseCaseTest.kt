package com.gamss.android.domain.emotion

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Suppress("MagicNumber")
class AnalyzeConversationEmotionUseCaseTest {

    /** 발화 텍스트 → (라벨, 신뢰도) 로 응답하는 fake 분류기. 미등록은 중립 0.0. */
    private fun classifierOf(responses: Map<String, Pair<String, Float>>) =
        object : EmotionClassifier {
            override suspend fun classify(text: String): ClassificationResult {
                val (label, conf) = responses[text] ?: ("중립" to 0.0f)
                return ClassificationResult(label, conf, mapOf(label to conf))
            }
        }

    @Test
    fun 메인화자의_최고신뢰_발화감정을_대표로_고른다() = runBlocking {
        val convo = """
            민수: 오늘 좀 힘들었어
            지영: 왜?
            민수: 부장이 소리질러서 진짜 화났어
        """.trimIndent()
        val useCase = AnalyzeConversationEmotionUseCase(
            classifierOf(
                mapOf(
                    "오늘 좀 힘들었어" to ("슬픔" to 0.55f),
                    "왜?" to ("당황" to 0.40f),
                    "부장이 소리질러서 진짜 화났어" to ("분노" to 0.93f),
                ),
            ),
        )
        val result = useCase(convo)
        assertEquals("민수", result?.mainSpeaker)
        assertEquals("분노", result?.emotion?.topLabel)
    }

    @Test
    fun 다수결이_아니라_신뢰도로_고른다() = runBlocking {
        // 저신뢰 잡담 2개(불안/당황) vs 고신뢰 슬픔 1개 → 슬픔이 대표.
        val convo = """
            나: ㅇㅇ
            나: 밥은 먹었어
            나: 강아지가 무지개다리를 건너서 너무 슬퍼
        """.trimIndent()
        val useCase = AnalyzeConversationEmotionUseCase(
            classifierOf(
                mapOf(
                    "ㅇㅇ" to ("불안" to 0.30f),
                    "밥은 먹었어" to ("당황" to 0.35f),
                    "강아지가 무지개다리를 건너서 너무 슬퍼" to ("슬픔" to 0.98f),
                ),
            ),
        )
        assertEquals("슬픔", useCase(convo)?.emotion?.topLabel)
    }

    @Test
    fun 일기_단문은_전체를_한_화자로_분류한다() = runBlocking {
        val diary = "오늘 시험에 합격해서 정말 기뻤다"
        val useCase = AnalyzeConversationEmotionUseCase(
            classifierOf(mapOf(diary to ("기쁨" to 0.99f))),
        )
        val result = useCase(diary)
        assertEquals(ConversationParser.DIARY_SPEAKER, result?.mainSpeaker)
        assertEquals("기쁨", result?.emotion?.topLabel)
    }

    @Test
    fun 공백_입력은_null() = runBlocking {
        val useCase = AnalyzeConversationEmotionUseCase(classifierOf(emptyMap()))
        assertNull(useCase("   "))
    }
}
