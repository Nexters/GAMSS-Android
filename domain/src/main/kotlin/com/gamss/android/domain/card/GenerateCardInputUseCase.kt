package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.map
import com.gamss.android.domain.emotion.ClassifyUserEmotionUseCase
import com.gamss.android.domain.emotion.EmotionResult
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class GenerateCardInputUseCase @Inject constructor(
    private val classifyUserEmotion: ClassifyUserEmotionUseCase,
    private val summarizeDiary: SummarizeDiaryUseCase,
) : UseCase<List<String>, AppResult<CardInput?>> {

    override suspend fun invoke(params: List<String>): AppResult<CardInput?> =
        when (val emotion = classifyUserEmotion(params)) {
            is AppResult.Failure -> emotion
            is AppResult.Success -> emotion.data?.let { withSummary(it, params) } ?: AppResult.Success(null)
        }

    private suspend fun withSummary(emotion: EmotionResult, userUtterances: List<String>): AppResult<CardInput?> =
        summarizeDiary(userUtterances).map { summary ->
            CardInput(
                emotion = emotion.label,
                character = emotion.character,
                summary = summary,
            )
        }
}
