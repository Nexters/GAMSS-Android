package com.gamss.android.feature.game.clicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickerGameControllerTest {

    @Test
    fun `반영된 입력만 true 를 돌려준다`() {
        val game = ClickerGameController()

        assertTrue(game.tap())
        assertEquals(1, game.score)
        assertFalse(game.isIdle)
        assertTrue(game.isPlaying)
    }

    @Test
    fun `끝난 뒤의 입력은 false 를 돌려주고 상태를 바꾸지 않는다`() {
        val finished = ClickerGameState(phase = ClickerGamePhase.Finished, score = 10, gauge = 1f)
        val game = ClickerGameController(finished)

        assertFalse(game.tap())
        assertEquals(finished, game.state)
    }

    @Test
    fun `다시하기는 대기 상태로 되돌린다`() {
        val game = ClickerGameController(ClickerGameState(phase = ClickerGamePhase.Playing, score = 5, gauge = 0.3f))

        game.replay()

        assertEquals(ClickerGameState(), game.state)
        assertTrue(game.isIdle)
    }

    @Test
    fun `접근성 게이지 값은 1퍼센트 단위로 반올림한다`() {
        val game = ClickerGameController(ClickerGameState(phase = ClickerGamePhase.Playing, gauge = 0.346f))

        assertEquals(0.35f, game.gaugeProgress, 0.0001f)
    }
}
