package com.gamss.android.domain.conversation

import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSummaryStoreTest {

    private class MarkingSummarizer : DiarySummarizer {
        val inputs = mutableListOf<String>()
        val calls get() = inputs.size

        override suspend fun summarize(text: String): String {
            inputs += text
            return "[요약$calls]"
        }
    }

    /** 예산을 넘기기 쉬워 청크 경계를 테스트로 만들 수 있다. */
    private object CharTokenCounter : UtteranceTokenCounter {
        override suspend fun count(text: String): Int = text.length
    }

    private object PassThroughSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = text
    }

    private object FailingSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = error("boom")
    }

    private fun store(
        summarizer: DiarySummarizer = MarkingSummarizer(),
        tokenCounter: UtteranceTokenCounter = CharTokenCounter,
    ) = ConversationSummaryStore(summarizer, tokenCounter)

    @Test
    fun 발화가_없으면_압축본도_없다() = runBlocking {
        assertNull(store().currentContextSummary())
    }

    @Test
    fun 최근_창_안에_다_들어가면_원문만_이어붙인다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        listOf("첫째", "둘째", "셋째").forEach { store.add(it) }

        assertEquals("첫째 둘째 셋째", store.currentContextSummary())
        assertEquals(0, summarizer.calls)
    }

    @Test
    fun 발화가_하나여도_그_발화만_남는다() = runBlocking {
        val store = store()

        store.add("혼잣말")

        assertEquals("혼잣말", store.currentContextSummary())
    }

    @Test
    fun 최근_창을_한_칸_넘어도_예산_전에는_요약하지_않는다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        listOf("첫째", "둘째", "셋째", "넷째").forEach { store.add(it) }

        assertEquals("첫째 둘째 셋째 넷째", store.currentContextSummary())
        assertEquals(0, summarizer.calls)
    }

    @Test
    fun 예산에_닿은_청크만_요약되고_남은_발화는_원문으로_유지된다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        store.add("주제")
        store.add("가".repeat(SUMMARY_CHUNK_TOKEN_BUDGET))
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }

        assertEquals(1, summarizer.calls)
        assertEquals("주제 [요약1] 최근1 최근2 최근3", store.currentContextSummary())
    }

    @Test
    fun 요약기에는_예산을_넘는_입력이_들어가지_않는다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        // 한 발화가 예산의 40%씩 차지해 청크 경계가 발화 중간에 걸린다.
        val chunky = "나".repeat(SUMMARY_CHUNK_TOKEN_BUDGET * 2 / 5)
        store.add("주제")
        repeat(8) { store.add(chunky) }
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }

        assertTrue("요약이 한 번도 안 돌았다", summarizer.calls > 0)
        summarizer.inputs.forEach {
            // 넘겨서 넣으면 요약기 입력 한계에서 잘려 그 발화가 압축본에서 사라진다.
            assertTrue("요약 입력이 예산 초과: ${it.length}", it.length <= SUMMARY_CHUNK_TOKEN_BUDGET)
        }
    }

    @Test
    fun 같은_청크를_두_번_요약하지_않는다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        store.add("주제")
        store.add("가".repeat(SUMMARY_CHUNK_TOKEN_BUDGET))
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }
        val callsAfterFirstChunk = summarizer.calls

        listOf("최근4", "최근5").forEach { store.add(it) }

        assertEquals(callsAfterFirstChunk, summarizer.calls)
        assertEquals("주제 [요약1] 최근1 최근2 최근3 최근4 최근5", store.currentContextSummary())
    }

    @Test
    fun 요약이_실패하면_원문으로_확정하고_다시_시도하지_않는다() = runBlocking {
        val failing = object : DiarySummarizer {
            var calls = 0
                private set

            override suspend fun summarize(text: String): String {
                calls++
                error("boom")
            }
        }
        val store = store(failing)

        store.add("주제")
        store.add("나".repeat(SUMMARY_CHUNK_TOKEN_BUDGET))
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }
        val callsAfterFirstChunk = failing.calls
        listOf("최근4", "최근5").forEach { store.add(it) }

        val summary = store.currentContextSummary()!!
        assertTrue(summary.startsWith("주제 나나나"))
        assertTrue(summary.endsWith("최근3 최근4 최근5"))
        // 실패한 청크를 열어 두면 전송마다 요약을 재시도해 지연이 누적된다.
        assertEquals(callsAfterFirstChunk, failing.calls)
    }

    @Test
    fun 상한을_넘기면_첫_발화와_최근_발화를_남기고_오래된_쪽을_버린다() = runBlocking {
        val store = store(PassThroughSummarizer)

        store.add("주제")
        repeat(6) { store.add("다".repeat(SUMMARY_CHUNK_TOKEN_BUDGET)) }
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }

        val summary = store.currentContextSummary()!!
        assertTrue("length=${summary.length}", summary.length <= MAX_CONTEXT_SUMMARY_LENGTH)
        assertTrue("첫 발화가 사라졌다", summary.startsWith("주제"))
        assertTrue("최근 발화가 사라졌다", summary.endsWith("최근1 최근2 최근3"))
    }

    @Test
    fun 요약이_계속_실패해도_최근_발화는_압축본에_남는다() = runBlocking {
        val store = store(FailingSummarizer)

        store.add("주제")
        repeat(30) { store.add("라".repeat(LONG_UTTERANCE_CHARS)) }
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }

        val summary = store.currentContextSummary()!!
        assertTrue("length=${summary.length}", summary.length <= MAX_CONTEXT_SUMMARY_LENGTH)
        assertTrue("첫 발화가 사라졌다", summary.startsWith("주제"))
        assertTrue("최근 발화가 사라졌다", summary.endsWith("최근1 최근2 최근3"))
    }

    @Test
    fun 토큰_계산이_실패해도_압축본을_만든다() = runBlocking {
        val failingCounter = object : UtteranceTokenCounter {
            override suspend fun count(text: String): Int = error("tokenizer dead")
        }
        val store = store(PassThroughSummarizer, failingCounter)

        store.add("주제")
        repeat(5) { store.add("마".repeat(LONG_UTTERANCE_CHARS)) }
        listOf("최근1", "최근2", "최근3").forEach { store.add(it) }

        val summary = store.currentContextSummary()!!
        assertTrue(summary.startsWith("주제"))
        assertTrue(summary.endsWith("최근1 최근2 최근3"))
    }

    @Test
    fun 복원은_히스토리를_요약하지_않는다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        store.restore(List(40) { "발화$it".repeat(20) })

        assertEquals(0, summarizer.calls)
        val summary = store.currentContextSummary()!!
        assertTrue("length=${summary.length}", summary.length <= MAX_CONTEXT_SUMMARY_LENGTH)
        assertTrue("최근 발화가 사라졌다", summary.endsWith("발화39".repeat(20)))
    }

    @Test
    fun 복원_뒤_새_발화는_이전_대화를_지우지_않는다() = runBlocking {
        val store = store(PassThroughSummarizer)

        store.restore(listOf("주제", "중간1", "중간2", "중간3"))
        store.add("새발화")

        val summary = store.currentContextSummary()!!
        assertTrue(summary.startsWith("주제"))
        assertTrue(summary.endsWith("새발화"))
    }

    @Test
    fun reset_하면_이전_대화가_남지_않는다() = runBlocking {
        val store = store()
        listOf("첫째", "둘째", "셋째", "넷째").forEach { store.add(it) }

        store.reset()
        assertNull(store.currentContextSummary())

        // reset 후에도 정상 동작해야 한다(재진입 복원이 같은 인스턴스를 다시 채운다).
        listOf("새첫째", "새둘째").forEach { store.add(it) }
        assertEquals("새첫째 새둘째", store.currentContextSummary())
    }

    private companion object {
        const val LONG_UTTERANCE_CHARS = 140
    }
}
