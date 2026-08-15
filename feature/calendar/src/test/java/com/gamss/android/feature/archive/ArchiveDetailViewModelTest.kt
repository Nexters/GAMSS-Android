package com.gamss.android.feature.archive

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardQueryRepository
import com.gamss.android.domain.card.GetCardsByDateUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveDetailViewModelTest {

    @Test
    fun `선택한 감정의 카드만 종이 목록으로 남긴다`() = runTest {
        val viewModel = viewModel(AppResult.Success(listOf(angerCard, joyCard)))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState {
                copy(
                    emotion = EmotionCharacter.ANGER,
                    isLoading = false,
                    cards = listOf(angerCard),
                )
            }
        }
    }

    @Test
    fun `카드 조회에 실패하면 오류 상태를 표시한다`() = runTest {
        val viewModel = viewModel(AppResult.Failure(IllegalStateException("network")))

        viewModel.test(this) {
            containerHost.load(EmotionCharacter.ANGER)

            expectState { copy(emotion = EmotionCharacter.ANGER) }
            expectState {
                copy(
                    emotion = EmotionCharacter.ANGER,
                    isLoading = false,
                    loadFailed = true,
                )
            }
        }
    }

    private fun viewModel(result: AppResult<List<Card>>) = ArchiveDetailViewModel(
        GetCardsByDateUseCase(FakeCardRepository(result)),
    )

    private companion object {
        val angerCard = Card(
            character = EmotionCharacter.ANGER,
            summary = "속상했던 하루",
            message = "속상했어요",
        )
        val joyCard = Card(
            character = EmotionCharacter.JOY,
            summary = "기분 좋은 하루",
            message = "기뻤어요",
        )
    }
}

private class FakeCardRepository(
    private val cardsResult: AppResult<List<Card>>,
) : CardQueryRepository {
    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> = cardsResult
}
