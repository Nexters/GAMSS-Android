package com.gamss.android.feature.game.clicker

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickerGameControllerTest {

    @Test
    fun `반영된 입력만 true 를 돌려준다`() {
        val game = ClickerGameController(ClickerGameState.playing())

        assertTrue(game.tap())
        assertEquals(1, game.score)
    }

    @Test
    fun `카운트다운 중 입력은 false 를 돌려준다`() {
        val game = ClickerGameController()

        assertFalse(game.tap())
        assertEquals(0, game.score)
    }

    @Test
    fun `끝난 뒤의 입력은 false 를 돌려주고 상태를 바꾸지 않는다`() {
        val finished = ClickerGameState.finished(score = 10)
        val game = ClickerGameController(finished)

        assertFalse(game.tap())
        assertEquals(finished, game.state)
    }

    @Test
    fun `끝나는 순간에 한 번만 점수를 알린다`() {
        val game = ClickerGameController(ClickerGameState.playing(score = 7, playedMs = GAME_DURATION_MS - 50L))
        val finishedScores = mutableListOf<Int>()

        game.advance(100L) { finishedScores += it }
        game.advance(100L) { finishedScores += it }

        assertTrue(game.isFinished)
        assertEquals(listOf(7), finishedScores)
    }

    @Test
    fun `끝난 상태로 복원되면 점수를 다시 알리지 않는다`() {
        val game = ClickerGameController(ClickerGameState.finished(score = 7))
        val finishedScores = mutableListOf<Int>()

        game.advance(100L) { finishedScores += it }

        assertEquals(emptyList<Int>(), finishedScores)
    }

    @Test
    fun `저장했다 복원해도 단계와 점수가 그대로다`() {
        val states = listOf(
            ClickerGameState().advance(1_200L),
            ClickerGameState.playing(score = 42, playedMs = 12_300L),
            ClickerGameState.finished(score = 99),
        )

        states.forEach { state ->
            val saved = with(ClickerGameController.Saver) { SaverScope { true }.save(ClickerGameController(state)) }
            val restored = ClickerGameController.Saver.restore(requireNotNull(saved))

            assertEquals(state, restored?.state)
        }
    }

    @Test
    fun `다시하기는 카운트다운부터 다시 시작한다`() {
        val game = ClickerGameController(ClickerGameState.finished(score = 5))

        game.replay()

        assertEquals(ClickerGameState(), game.state)
        assertEquals(3, game.countdownSecond)
    }

    @Test
    fun `접근성 게이지 값은 1퍼센트 단위로 반올림한다`() {
        val game = ClickerGameController(ClickerGameState.playing(score = 160))

        // 160 / 464 = 0.3448...
        assertEquals(0.34f, game.gaugeProgress, 0.0001f)
    }
}
