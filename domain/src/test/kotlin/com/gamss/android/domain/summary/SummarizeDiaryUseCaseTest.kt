package com.gamss.android.domain.summary

import com.gamss.android.domain.assertSuccess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummarizeDiaryUseCaseTest {

    /** 호출 여부를 기록하는 fake 요약기. */
    private class RecordingSummarizer(val transform: (String) -> String) : DiarySummarizer {
        var called = false
            private set

        override suspend fun summarize(text: String): String {
            called = true
            return transform(text)
        }
    }

    @Test
    fun 충분히_긴_입력은_발화를_이어_요약기를_호출한다() = runBlocking {
        val summarizer = RecordingSummarizer { "요약<$it>" }
        val useCase = SummarizeDiaryUseCase(summarizer)
        val result = useCase(
            listOf("오늘 회사에서 부장님한테 크게 혼났는데", "   ", "딱히 내 잘못도 아니어서 하루종일 억울하고 기분이 안 좋았다"),
        ).assertSuccess()
        assertTrue(summarizer.called)
        assertEquals("요약<오늘 회사에서 부장님한테 크게 혼났는데 딱히 내 잘못도 아니어서 하루종일 억울하고 기분이 안 좋았다>", result)
    }

    @Test
    fun 짧은_입력은_요약기를_호출하지_않고_원문을_그대로_돌려준다() = runBlocking {
        val summarizer = RecordingSummarizer { "요약<$it>" }
        val useCase = SummarizeDiaryUseCase(summarizer)
        val result = useCase(listOf("오늘 억울한 일이 있었어", "그냥 쉬려고")).assertSuccess()
        assertFalse(summarizer.called)
        assertEquals("오늘 억울한 일이 있었어 그냥 쉬려고", result)
    }

    @Test
    fun 공백_입력은_Success_null() = runBlocking {
        val summarizer = RecordingSummarizer { it }
        val useCase = SummarizeDiaryUseCase(summarizer)
        assertNull(useCase(listOf("", "   ")).assertSuccess())
        assertFalse(summarizer.called)
    }

    @Test
    fun 연달아_같은_발화는_한_번만_남긴다() = runBlocking {
        val useCase = SummarizeDiaryUseCase(RecordingSummarizer { it })

        val result = useCase(listOf("나 배고파", "배고파", "배고파", "그래서 뭐 먹지", "배고파")).assertSuccess()

        assertEquals("나 배고파 배고파 그래서 뭐 먹지 배고파", result)
    }
}
