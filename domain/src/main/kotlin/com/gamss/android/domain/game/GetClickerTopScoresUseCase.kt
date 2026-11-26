package com.gamss.android.domain.game

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class GetClickerTopScoresUseCase @Inject constructor(
    private val history: ClickerScoreHistory,
) : NoParamUseCase<List<ClickerScoreRecord>> {

    override suspend fun invoke(): List<ClickerScoreRecord> = history.load()
}
