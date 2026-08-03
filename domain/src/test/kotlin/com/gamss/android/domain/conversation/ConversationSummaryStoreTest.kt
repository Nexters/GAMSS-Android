package com.gamss.android.domain.conversation

import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSummaryStoreTest {

    /** 요약을 눈에 보이게 표시해 어느 구간이 압축됐는지 확인한다. */
    private class MarkingSummarizer : DiarySummarizer {
        var calls = 0
            private set

        override suspend fun summarize(text: String): String {
            calls++
            return "[요약$calls]"
        }
    }

    /** 글자 하나를 토큰 하나로 센다. 예산을 넘기기 쉬워 청크 경계를 테스트로 만들 수 있다. */
    private object CharTokenCounter : UtteranceTokenCounter {
        override suspend fun count(text: String): Int = text.length
    }

    private fun store(summarizer: DiarySummarizer = MarkingSummarizer()) =
        ConversationSummaryStore(summarizer, CharTokenCounter)

    @Test
    fun 발화가_없으면_압축본도_없다() {
        assertNull(store().current())
    }

    @Test
    fun 최근_창_안에_다_들어가면_원문만_이어붙인다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        store.addAll(listOf("첫째", "둘째", "셋째"))

        // 3개는 모두 최근 창이라 첫 발화를 따로 붙이지 않는다(중복 방지).
        assertEquals("첫째 둘째 셋째", store.current())
        assertEquals(0, summarizer.calls)
    }

    @Test
    fun 최근_창을_넘어서면_첫_발화를_따로_남긴다() = runBlocking {
        val store = store()

        store.addAll(listOf("첫째", "둘째", "셋째", "넷째"))

        // 첫째는 원문으로 남고 둘째부터가 최근 창이다.
        assertEquals("첫째 둘째 셋째 넷째", store.current())
    }

    @Test
    fun 예산에_닿은_청크만_요약되고_남은_발화는_원문으로_유지된다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        // 첫 발화 + 예산(512자)을 채우는 긴 발화 + 최근 3턴
        store.add("주제")
        store.add("가".repeat(SUMMARY_CHUNK_TOKEN_BUDGET))
        store.addAll(listOf("최근1", "최근2", "최근3"))

        val summary = store.current()
        assertEquals(1, summarizer.calls)
        assertEquals("주제 [요약1] 최근1 최근2 최근3", summary)
    }

    @Test
    fun 같은_청크를_두_번_요약하지_않는다() = runBlocking {
        val summarizer = MarkingSummarizer()
        val store = store(summarizer)

        store.add("주제")
        store.add("가".repeat(SUMMARY_CHUNK_TOKEN_BUDGET))
        store.addAll(listOf("최근1", "최근2", "최근3"))
        val callsAfterFirstChunk = summarizer.calls

        store.addAll(listOf("최근4", "최근5"))
        store.current()

        // 새 발화가 예산을 채우지 않는 한 요약기는 다시 돌지 않는다.
        assertEquals(callsAfterFirstChunk, summarizer.calls)
    }

    @Test
    fun 요약이_실패하면_원문을_남긴다() = runBlocking {
        val failing = object : DiarySummarizer {
            override suspend fun summarize(text: String): String = error("boom")
        }
        val store = store(failing)

        store.add("주제")
        store.add("나".repeat(SUMMARY_CHUNK_TOKEN_BUDGET))
        store.addAll(listOf("최근1", "최근2", "최근3"))

        val summary = store.current()
        assertTrue(summary!!.startsWith("주제 나나나"))
        assertTrue(summary.endsWith("최근1 최근2 최근3"))
    }

    @Test
    fun 상한을_넘기면_잘라서_전송이_거절되지_않게_한다() = runBlocking {
        val store = store(PassThroughSummarizer)

        store.add("주제")
        repeat(6) { store.add("다".repeat(SUMMARY_CHUNK_TOKEN_BUDGET)) }
        store.addAll(listOf("최근1", "최근2", "최근3"))

        val summary = store.current()
        assertTrue("length=${summary!!.length}", summary.length <= MAX_CONTEXT_SUMMARY_LENGTH)
    }

    @Test
    fun reset_하면_이전_대화가_남지_않는다() = runBlocking {
        val store = store()
        store.addAll(listOf("첫째", "둘째", "셋째"))

        store.reset()

        assertNull(store.current())
    }

    private object PassThroughSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = text
    }
}
