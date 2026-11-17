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
    fun `첫 탭에 게임이 시작되고 점수와 게이지가 오른다`() {
        val state = ClickerGameState().tap()

        assertEquals(ClickerGamePhase.Playing, state.phase)
        assertEquals(1, state.score)
        assertEquals(GAUGE_PER_TAP, state.gauge, DELTA)
    }

    @Test
    fun `진행 중에는 시간에 따라 게이지가 줄고 0 아래로 내려가지 않는다`() {
        val tapped = ClickerGameState().tap()

        val drained = tapped.advance(100L)
        assertEquals(GAUGE_PER_TAP - GAUGE_DRAIN_PER_SECOND * 0.1f, drained.gauge, DELTA)
        assertEquals(100L, drained.elapsedMs)

        assertEquals(0f, tapped.advance(5_000L).gauge, DELTA)
    }

    @Test
    fun `게이지가 가득 차면 끝난다`() {
        val state = ClickerGameState(phase = ClickerGamePhase.Playing, score = 49, gauge = 1f - GAUGE_PER_TAP).tap()

        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(1f, state.gauge, DELTA)
        assertEquals(50, state.score)
    }

    @Test
    fun `시간이 흐르지 않으면 50번 만에 가득 찬다`() {
        var state = ClickerGameState()
        repeat(49) { state = state.tap() }
        assertEquals(ClickerGamePhase.Playing, state.phase)

        state = state.tap()
        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(1f, state.gauge, DELTA)
    }

    @Test
    fun `시간이 다 되면 끝나고 남은 시간은 0초다`() {
        val state = ClickerGameState().tap().advance(GAME_DURATION_MS + 500L)

        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(GAME_DURATION_MS, state.elapsedMs)
        assertEquals(0L, state.remainingTenths)
    }

    @Test
    fun `끝난 뒤에는 탭도 시간도 반영하지 않는다`() {
        val finished = ClickerGameState(
            phase = ClickerGamePhase.Finished,
            score = 10,
            gauge = 0.4f,
            elapsedMs = GAME_DURATION_MS,
        )

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
