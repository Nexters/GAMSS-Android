package com.gamss.android.domain.game

import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

const val CLICKER_TOP_SCORE_COUNT = 10

/** 점수가 같으면 먼저 낸 기록이 앞선다. 순위 밖으로 밀린 기록은 남기지 않는다. */
class RecordClickerScoreUseCase @Inject constructor(
    private val history: ClickerScoreHistory,
) : UseCase<ClickerScoreRecord, List<ClickerScoreRecord>> {

    override suspend fun invoke(params: ClickerScoreRecord): List<ClickerScoreRecord> =
        history.update { records ->
            (records + params)
                .sortedWith(compareByDescending<ClickerScoreRecord> { it.score }.thenBy { it.playedAtMillis })
                .take(CLICKER_TOP_SCORE_COUNT)
        }
}
