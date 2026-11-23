package com.gamss.android.feature.game.clicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ClickerGameStateTest {

    @Test
    fun `대기 중에는 시간이 흐르지 않는다`() {
        val state = ClickerGameState().advance(1_000L)

        assertEquals(ClickerGameState(), state)
    }

    @Test
    fun `첫 탭에 게임이 시작되고 점수가 오른다`() {
        val state = ClickerGameState().tap()

        assertEquals(ClickerGamePhase.Playing, state.phase)
        assertEquals(1, state.score)
    }

    @Test
    fun `게이지는 시간이 흘러도 빠지지 않는다`() {
        val state = ClickerGameState(phase = ClickerGamePhase.Playing, score = 100).advance(5_000L)

        assertEquals(100f / FULL_GAUGE_SCORE, state.gauge, DELTA)
    }

    @Test
    fun `시안의 232점은 게이지 절반이다`() {
        assertEquals(0.5f, ClickerGameState(score = 232).gauge, DELTA)
    }

    @Test
    fun `게이지가 가득 차도 점수는 계속 오른다`() {
        val state = ClickerGameState(phase = ClickerGamePhase.Playing, score = FULL_GAUGE_SCORE).tap()

        assertEquals(ClickerGamePhase.Playing, state.phase)
        assertEquals(FULL_GAUGE_SCORE + 1, state.score)
        assertEquals(1f, state.gauge, DELTA)
    }

    @Test
    fun `30초가 지나면 끝나고 남은 시간은 0초다`() {
        val state = ClickerGameState().tap().advance(GAME_DURATION_MS + 500L)

        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(GAME_DURATION_MS, state.elapsedMs)
        assertEquals(0L, state.remainingTenths)
    }

    @Test
    fun `끝난 뒤에는 탭도 시간도 반영하지 않는다`() {
        val finished = ClickerGameState(phase = ClickerGamePhase.Finished, score = 10, elapsedMs = GAME_DURATION_MS)

        assertSame(finished, finished.tap())
        assertSame(finished, finished.advance(100L))
    }

    @Test
    fun `남은 시간은 0점1초 단위로 올림한다`() {
        assertEquals(300L, ClickerGameState().remainingTenths)
        assertEquals(300L, ClickerGameState(elapsedMs = 1L).remainingTenths)
        assertEquals(299L, ClickerGameState(elapsedMs = 100L).remainingTenths)
        assertEquals(1L, ClickerGameState(elapsedMs = GAME_DURATION_MS - 1L).remainingTenths)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
