package com.gamss.android.domain.emotion

import com.gamss.android.domain.assertSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    /** 특정 발화에서만 던지는 fake. 온디바이스 모델 실패를 흉내 낸다. */
    private class RejectingClassifier(
        private val rejected: String,
        private val rejectCount: Int = Int.MAX_VALUE,
    ) : EmotionClassifier {
        private val delegate = KeywordClassifier()
        private var rejections = 0

        var calls = 0
            private set

        override suspend fun classify(text: String): ClassificationResult {
            calls++
            if (text == rejected && rejections < rejectCount) {
                rejections++
                error("model load failed")
            }
            return delegate.classify(text)
        }
    }

    /** 분류가 도는 순간을 붙들 수 있는 fake. */
    private class GatedClassifier(
        private val started: CompletableDeferred<Unit>,
        private val release: CompletableDeferred<Unit>,
    ) : EmotionClassifier {
        private val delegate = KeywordClassifier()

        var calls = 0
            private set

        override suspend fun classify(text: String): ClassificationResult {
            calls++
            started.complete(Unit)
            release.await()
            return delegate.classify(text)
        }
    }

    private val utterances = listOf("오늘 분노 가득한 하루", "조금 불안 하기도 하고", "그래도 분노 가 제일 크다")

    @Test
    fun 증분_누적_결과가_한꺼번에_분류한_결과와_같다() = runBlocking {
        val batch = ClassifyUserEmotionUseCase(KeywordClassifier())(utterances).assertSuccess()

        // 대화가 진행되며 발화가 하나씩 늘어나는 실제 호출 방식.
        val accumulator = ConversationEmotionAccumulator(KeywordClassifier())
        utterances.forEach {
            accumulator.append(it)
            accumulator.classifyPending()
        }

        assertEquals(batch, accumulator.result())
        assertEquals(EmotionLabel.ANGER, accumulator.result()?.label)
    }

    @Test
    fun 발화마다_분류를_한_번씩만_한다() = runBlocking {
        val classifier = KeywordClassifier()
        val accumulator = ConversationEmotionAccumulator(classifier)

        accumulator.restore(utterances)
        accumulator.classifyPending()
        accumulator.classifyPending()

        assertEquals(utterances.size, classifier.calls)
    }

    @Test
    fun 분류에_실패하면_그_자리에서_멈추고_다음에_이어받는다() = runBlocking {
        val classifier = RejectingClassifier(rejected = FAILING.first(), rejectCount = 1)
        val accumulator = ConversationEmotionAccumulator(classifier)
        accumulator.restore(FAILING)

        assertFalse(accumulator.classifyPending())
        assertNull(accumulator.result())
        // 실패한 자리에서 멈췄으므로 뒤 발화는 아직 분류하지 않는다.
        assertEquals(1, classifier.calls)

        assertTrue(accumulator.classifyPending())
        assertEquals(FAILING.size + 1, classifier.calls)
        assertEquals(EmotionLabel.ANGER, accumulator.result()?.label)
    }

    @Test
    fun 같은_자리에서_계속_실패하면_건너뛰고_뒤_발화를_누적한다() = runBlocking {
        val accumulator = ConversationEmotionAccumulator(RejectingClassifier(rejected = FAILING.first()))
        accumulator.restore(FAILING)

        assertFalse(accumulator.classifyPending())
        // 무한히 붙들면 뒤 발화가 영원히 누적되지 않는다.
        assertTrue(accumulator.classifyPending())

        assertEquals(EmotionLabel.ANGER, accumulator.result()?.label)
    }

    @Test
    fun 공백_발화는_누적하지_않는다() = runBlocking {
        val classifier = KeywordClassifier()
        val accumulator = ConversationEmotionAccumulator(classifier)

        accumulator.restore(listOf("   ", "", "\n"))
        accumulator.append("  ")
        accumulator.classifyPending()

        assertEquals(0, classifier.calls)
        assertNull(accumulator.result())
    }

    @Test
    fun 아무것도_더하지_않으면_결과가_없다() = runBlocking {
        assertNull(ConversationEmotionAccumulator(KeywordClassifier()).result())
    }

    @Test
    fun restore_하면_이전_누적이_사라진다() = runBlocking {
        val accumulator = ConversationEmotionAccumulator(KeywordClassifier())
        accumulator.restore(utterances)
        accumulator.classifyPending()

        accumulator.restore(listOf("완전히 기쁨 뿐인 하루"))
        accumulator.classifyPending()

        assertEquals(EmotionLabel.JOY, accumulator.result()?.label)
    }

    @Test
    fun reset_하면_이전_누적이_사라진다() = runBlocking {
        val accumulator = ConversationEmotionAccumulator(KeywordClassifier())
        accumulator.restore(utterances)
        accumulator.classifyPending()

        accumulator.reset()

        assertNull(accumulator.result())
    }

    @Test
    fun 순서가_달라도_대표_감정은_같다() = runBlocking {
        val forward = ConversationEmotionAccumulator(KeywordClassifier())
        forward.restore(utterances)
        forward.classifyPending()

        val reversed = ConversationEmotionAccumulator(KeywordClassifier())
        reversed.restore(utterances.reversed())
        reversed.classifyPending()

        assertEquals(forward.result()?.label, reversed.result()?.label)
    }

    @Test
    fun 분류가_도는_동안에도_새_발화를_받는다() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val classifier = GatedClassifier(started, release)
        val accumulator = ConversationEmotionAccumulator(classifier)
        accumulator.append("오늘 분노 가득한 하루")

        val classification = launch { accumulator.classifyPending() }
        started.await()

        // 분류와 적재가 같은 잠금을 쓰면 여기서 막힌다. 전송이 추론을 기다리게 된다.
        withTimeout(APPEND_TIMEOUT_MILLIS) { accumulator.append("그래도 분노 가 남는다") }

        release.complete(Unit)
        classification.join()
        accumulator.classifyPending()

        assertEquals(2, classifier.calls)
        assertEquals(EmotionLabel.ANGER, accumulator.result()?.label)
    }

    @Test
    fun 분류가_도는_동안_초기화되면_앞_대화_점수를_새_대화에_섞지_않는다() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val accumulator = ConversationEmotionAccumulator(GatedClassifier(started, release))
        accumulator.append("오늘 분노 가득한 하루")

        val classification = launch { accumulator.classifyPending() }
        started.await()

        // 앞 대화를 분류하는 중에 새 대화가 열린다. 늦게 도착한 점수가 여기에 얹히면 안 된다.
        accumulator.reset()
        accumulator.append("조금 불안 하기도 하고")

        release.complete(Unit)
        classification.join()
        accumulator.classifyPending()

        assertEquals(EmotionLabel.ANXIETY, accumulator.result()?.label)
    }

    private companion object {
        val FAILING = listOf("첫 발화에서 분류가 죽는다", "그래도 분노 가 남는다")
        const val APPEND_TIMEOUT_MILLIS = 1_000L
    }
}
