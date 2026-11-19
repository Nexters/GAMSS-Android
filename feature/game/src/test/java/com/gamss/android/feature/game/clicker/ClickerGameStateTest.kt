package com.gamss.android.feature.game.clicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ClickerGameStateTest {

    @Test
    fun `대기 중에는 시간이 흐르지 않는다`() {
        val state = ClickerGameState().advance(1_000L)

        assertEquals(ClickerGameState(), state)
        assertEquals(ClickerRecord.Running(0L), state.record)
    }

    @Test
    fun `첫 탭에 게임이 시작되고 게이지가 오른다`() {
        val state = ClickerGameState().tap()

        assertEquals(ClickerGamePhase.Playing, state.phase)
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
    fun `진행 중 기록은 흐른 시간을 0점1초 단위로 내림한다`() {
        val state = ClickerGameState(phase = ClickerGamePhase.Playing, elapsedMs = 12_399L)

        assertEquals(ClickerRecord.Running(123L), state.record)
    }

    @Test
    fun `가득 채우면 걸린 시간을 0점01초 단위 기록으로 남긴다`() {
        val state = ClickerGameState(
            phase = ClickerGamePhase.Playing,
            gauge = 1f - GAUGE_PER_TAP,
            elapsedMs = 12_345L,
        ).tap()

        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(1f, state.gauge, DELTA)
        assertEquals(ClickerRecord.Cleared(1_234L), state.record)
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
    fun `제한 시간이 지나면 시간 초과로 끝난다`() {
        val state = ClickerGameState().tap().advance(GAME_DURATION_MS + 500L)

        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(GAME_DURATION_MS, state.elapsedMs)
        assertEquals(ClickerRecord.TimeOver, state.record)
    }

    @Test
    fun `끝난 뒤에는 탭도 시간도 반영하지 않는다`() {
        val finished = ClickerGameState(phase = ClickerGamePhase.Finished, gauge = 0.4f, elapsedMs = GAME_DURATION_MS)

        assertSame(finished, finished.tap())
        assertSame(finished, finished.advance(100L))
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
