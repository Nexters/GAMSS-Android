package com.gamss.android.domain.summary

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummarizeDiaryUseCaseTest {

    /** 입력 텍스트를 변환해 돌려주는 fake 요약기. */
    private fun summarizerOf(transform: (String) -> String) =
        object : DiarySummarizer {
            override suspend fun summarize(text: String): String = transform(text)
        }

    @Test
    fun 여러_발화를_공백으로_이어_요약한다() = runBlocking {
        val useCase = SummarizeDiaryUseCase(summarizerOf { "요약<$it>" })
        val result = useCase(listOf("오늘 회사에서", "   ", "너무 힘들었어"))
        assertEquals("요약<오늘 회사에서 너무 힘들었어>", result)
    }

    @Test
    fun 공백_입력은_null() = runBlocking {
        val useCase = SummarizeDiaryUseCase(summarizerOf { it })
        assertNull(useCase(listOf("", "   ")))
    }
}
