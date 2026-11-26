package com.gamss.android.feature.game.clicker

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.game.ClickerScoreRecord
import com.gamss.android.domain.game.GetClickerTopScoresUseCase
import com.gamss.android.domain.game.RecordClickerScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ClickerGameViewModel @Inject constructor(
    private val getTopScores: GetClickerTopScoresUseCase,
    private val recordScore: RecordClickerScoreUseCase,
) : ViewModel(), ContainerHost<ClickerScoreState, Nothing> {

    // 끝난 상태로 복원된 화면은 기록을 다시 남기지 않으므로, 팝업에 보일 목록을 저장소에서 읽어 둔다.
    override val container = container<ClickerScoreState, Nothing>(ClickerScoreState()) {
        val topScores = getTopScores()
        reduce { state.copy(topScores = topScores) }
    }

    fun onGameFinished(score: Int, playedAtMillis: Long = System.currentTimeMillis()) = intent {
        val topScores = recordScore(ClickerScoreRecord(score = score, playedAtMillis = playedAtMillis))
        reduce { state.copy(topScores = topScores) }
    }
}
