package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.getOrNull
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/**
 * 대화 발화를 요약해 카드를 만든다. 요약이 비면 카드 제목이 없어 만들 수 없으므로 실패로 접는다.
 *
 * 대화 종료와 묶지 않는다. 종료는 되돌릴 수 없어서, 종료는 됐고 카드만 실패한 상태를 화면이 들고 있어야 한다.
 */
class CreateConversationCardUseCase @Inject constructor(
    private val summarizeDiary: SummarizeDiaryUseCase,
    private val createCard: CreateCardUseCase,
) : UseCase<CreateConversationCardUseCase.Params, AppResult<Card>> {

    data class Params(
        val conversationId: Long,
        val character: EmotionCharacter,
        val utterances: List<String>,
    )

    override suspend fun invoke(params: Params): AppResult<Card> {
        val summary = summarizeDiary(params.utterances).getOrNull()
        if (summary.isNullOrBlank()) {
            return AppResult.Failure(CardNotRetryableException.NoSummary())
        }
        return createCard(
            CreateCardUseCase.Params(
                conversationId = params.conversationId,
                character = params.character,
                summary = summary,
            ),
        )
    }
}
