package com.gamss.android.domain.emotion

import com.gamss.android.domain.assertSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationEmotionAccumulatorTest {

    /** 발화에 담긴 감정 단어로 점수를 만드는 fake. 호출 횟수도 센다. */
    private class KeywordClassifier : EmotionClassifier {
        var calls = 0
            private set

        override suspend fun classify(text: String): ClassificationResult {
            calls++
            val scores = EmotionLabel.entries.associate { label ->
                label.koLabel to if (text.contains(label.koLabel)) HIGH else LOW
            }
            val top = scores.maxBy { it.value }
            return ClassificationResult(top.key, top.value, scores)
        }

        private companion object {
            const val HIGH = 0.9f
            const val LOW = 0.02f
        }
    }

    private val utterances = listOf("오늘 분노 가득한 하루", "조금 불안 하기도 하고", "그래도 분노 가 제일 크다")

    @Test
    fun 증분_누적_결과가_한꺼번에_분류한_결과와_같다() = runBlocking {
        val batch = ClassifyUserEmotionUseCase(KeywordClassifier())(utterances).assertSuccess()

        val accumulator = ConversationEmotionAccumulator(KeywordClassifier())
        utterances.forEach { accumulator.add(it) }
        val incremental = accumulator.result()

        assertEquals(batch, incremental)
        assertEquals(EmotionLabel.ANGER, incremental?.label)
    }

    @Test
    fun 발화마다_분류를_한_번씩만_한다() = runBlocking {
        val classifier = KeywordClassifier()
        val accumulator = ConversationEmotionAccumulator(classifier)

        accumulator.addAll(utterances)
        accumulator.result()
        accumulator.result()

        assertEquals(utterances.size, classifier.calls)
    }

    @Test
    fun 공백_발화는_누적하지_않는다() = runBlocking {
        val classifier = KeywordClassifier()
        val accumulator = ConversationEmotionAccumulator(classifier)

        accumulator.addAll(listOf("   ", "", "\n"))

        assertEquals(0, classifier.calls)
        assertNull(accumulator.result())
    }

    @Test
    fun 아무것도_더하지_않으면_결과가_없다() {
        assertNull(ConversationEmotionAccumulator(KeywordClassifier()).result())
    }

    @Test
    fun reset_하면_이전_누적이_사라진다() = runBlocking {
        val accumulator = ConversationEmotionAccumulator(KeywordClassifier())
        accumulator.addAll(utterances)

        accumulator.reset()
        accumulator.add("완전히 기쁨 뿐인 하루")

        assertEquals(EmotionLabel.JOY, accumulator.result()?.label)
    }

    @Test
    fun 순서가_달라도_대표_감정은_같다() = runBlocking {
        val forward = ConversationEmotionAccumulator(KeywordClassifier())
        forward.addAll(utterances)

        val reversed = ConversationEmotionAccumulator(KeywordClassifier())
        reversed.addAll(utterances.reversed())

        assertEquals(forward.result()?.label, reversed.result()?.label)
    }
}
