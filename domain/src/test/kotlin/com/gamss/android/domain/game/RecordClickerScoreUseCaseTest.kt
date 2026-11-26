package com.gamss.android.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordClickerScoreUseCaseTest {

    private val history = FakeClickerScoreHistory()
    private val recordScore = RecordClickerScoreUseCase(history)

    @Test
    fun `점수가 높은 순으로 정렬한다`() = runTest {
        recordScore(ClickerScoreRecord(score = 10, playedAtMillis = 1L))
        recordScore(ClickerScoreRecord(score = 30, playedAtMillis = 2L))
        val result = recordScore(ClickerScoreRecord(score = 20, playedAtMillis = 3L))

        assertEquals(listOf(30, 20, 10), result.map { it.score })
        assertEquals(result, history.load())
    }

    @Test
    fun `점수가 같으면 먼저 낸 기록이 앞선다`() = runTest {
        recordScore(ClickerScoreRecord(score = 10, playedAtMillis = 2L))
        val result = recordScore(ClickerScoreRecord(score = 10, playedAtMillis = 1L))

        assertEquals(listOf(1L, 2L), result.map { it.playedAtMillis })
    }

    @Test
    fun `10개를 넘으면 순위 밖 기록을 버린다`() = runTest {
        repeat(CLICKER_TOP_SCORE_COUNT) { index ->
            recordScore(ClickerScoreRecord(score = 100 + index, playedAtMillis = index.toLong()))
        }

        val result = recordScore(ClickerScoreRecord(score = 1, playedAtMillis = 99L))

        assertEquals(CLICKER_TOP_SCORE_COUNT, result.size)
        assertEquals(100, result.last().score)
    }

    @Test
    fun `10개가 찼어도 더 높은 점수는 들어가고 가장 낮은 기록이 빠진다`() = runTest {
        repeat(CLICKER_TOP_SCORE_COUNT) { index ->
            recordScore(ClickerScoreRecord(score = 100 + index, playedAtMillis = index.toLong()))
        }

        val result = recordScore(ClickerScoreRecord(score = 500, playedAtMillis = 99L))

        assertEquals(500, result.first().score)
        assertEquals(101, result.last().score)
    }

    private class FakeClickerScoreHistory : ClickerScoreHistory {
        private var records = emptyList<ClickerScoreRecord>()

        override suspend fun load(): List<ClickerScoreRecord> = records

        override suspend fun update(
            transform: (List<ClickerScoreRecord>) -> List<ClickerScoreRecord>,
        ): List<ClickerScoreRecord> = transform(records).also { records = it }
    }
}
