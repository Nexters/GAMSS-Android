package com.gamss.android.feature.game.clicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ClickerGameStateTest {

    @Test
    fun `카운트다운으로 시작하고 3 2 1 을 센다`() {
        val state = ClickerGameState()

        assertEquals(ClickerGamePhase.Countdown, state.phase)
        assertEquals(3, state.countdownSecond)
        assertEquals(3, state.advance(999L).countdownSecond)
        assertEquals(2, state.advance(1_000L).countdownSecond)
        assertEquals(1, state.advance(COUNTDOWN_MS - 1L).countdownSecond)
    }

    @Test
    fun `카운트다운이 끝나면 게임이 시작되고 숫자는 사라진다`() {
        val state = ClickerGameState().advance(COUNTDOWN_MS)

        assertEquals(ClickerGamePhase.Playing, state.phase)
        assertNull(state.countdownSecond)
    }

    @Test
    fun `카운트다운 중에는 탭을 세지 않는다`() {
        val countdown = ClickerGameState()

        assertSame(countdown, countdown.tap())
    }

    @Test
    fun `카운트다운 중에는 남은 시간이 30초로 멈춰 있다`() {
        assertEquals(300L, ClickerGameState().remainingTenths)
        assertEquals(300L, ClickerGameState().advance(2_500L).remainingTenths)
    }

    @Test
    fun `게임 중 탭마다 점수가 오른다`() {
        val state = ClickerGameState.playing().tap().tap()

        assertEquals(2, state.score)
    }

    @Test
    fun `게이지는 시간이 흘러도 빠지지 않는다`() {
        val state = ClickerGameState.playing(score = 100).advance(5_000L)

        assertEquals(100f / FULL_GAUGE_SCORE, state.gauge, DELTA)
    }

    @Test
    fun `시안의 232점은 게이지 절반이다`() {
        assertEquals(0.5f, ClickerGameState(score = 232).gauge, DELTA)
    }

    @Test
    fun `게이지가 가득 차도 점수는 계속 오른다`() {
        val state = ClickerGameState.playing(score = FULL_GAUGE_SCORE).tap()

        assertEquals(ClickerGamePhase.Playing, state.phase)
        assertEquals(FULL_GAUGE_SCORE + 1, state.score)
        assertEquals(1f, state.gauge, DELTA)
    }

    @Test
    fun `30초가 지나면 끝나고 남은 시간은 0초다`() {
        val state = ClickerGameState.playing().tap().advance(GAME_DURATION_MS + 500L)

        assertEquals(ClickerGamePhase.Finished, state.phase)
        assertEquals(ClickerGameState.finished(score = 1), state)
        assertEquals(0L, state.remainingTenths)
    }

    @Test
    fun `끝난 뒤에는 탭도 시간도 반영하지 않는다`() {
        val finished = ClickerGameState.finished(score = 10)

        assertSame(finished, finished.tap())
        assertSame(finished, finished.advance(100L))
    }

    @Test
    fun `남은 시간은 0점1초 단위로 올림한다`() {
        assertEquals(300L, ClickerGameState.playing(playedMs = 1L).remainingTenths)
        assertEquals(299L, ClickerGameState.playing(playedMs = 100L).remainingTenths)
        assertEquals(1L, ClickerGameState.playing(playedMs = GAME_DURATION_MS - 1L).remainingTenths)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
