package com.gamss.android.feature.game.clicker

import com.gamss.android.domain.game.ClickerScoreRecord
import com.gamss.android.domain.game.GetClickerTopScoresUseCase
import com.gamss.android.domain.game.RecordClickerScoreUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

class ClickerGameViewModelTest {

    private val getTopScores: GetClickerTopScoresUseCase = mockk()
    private val recordScore: RecordClickerScoreUseCase = mockk()

    private fun viewModel() = ClickerGameViewModel(getTopScores, recordScore)

    @Test
    fun `처음 열면 저장된 기록을 읽어 둔다`() = runTest {
        val saved = listOf(ClickerScoreRecord(score = 50, playedAtMillis = 1L))
        coEvery { getTopScores() } returns saved

        viewModel().test(this) {
            runOnCreate()
            expectState { copy(topScores = saved) }
        }
    }

    @Test
    fun `게임이 끝나면 점수를 한 번 기록하고 갱신된 순위를 보여 준다`() = runTest {
        val record = ClickerScoreRecord(score = 120, playedAtMillis = 2L)
        val updated = listOf(record)
        coEvery { recordScore(record) } returns updated

        viewModel().test(this) {
            containerHost.onGameFinished(score = 120, playedAtMillis = 2L)
            expectState { copy(topScores = updated) }
        }

        coVerify(exactly = 1) { recordScore(record) }
    }
}
