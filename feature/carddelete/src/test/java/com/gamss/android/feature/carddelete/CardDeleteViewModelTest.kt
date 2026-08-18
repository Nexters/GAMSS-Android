package com.gamss.android.feature.carddelete

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.DeleteAllCardsUseCase
import com.gamss.android.domain.card.DeleteCardUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class CardDeleteViewModelTest {

    private val deleteAllCards: DeleteAllCardsUseCase = mockk()
    private val deleteCard: DeleteCardUseCase = mockk()

    @Test
    fun `여섯 번째 탭에서 모든 카드를 한 번 삭제하고 완료 상태가 된다`() = runTest {
        coEvery { deleteAllCards() } returns AppResult.Success(Unit)

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS - 1) { tapIndex ->
                containerHost.onShredTap(null)
                expectState { copy(shredTapCount = tapIndex + 1) }
            }

            containerHost.onShredTap(null)
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS) }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
            expectSideEffect(CardDeleteSideEffect.ShredSuccess)
        }

        coVerify(exactly = 1) { deleteAllCards() }
    }

    @Test
    fun `삭제에 실패하면 초기 상태로 돌아가 다시 시도할 수 있다`() = runTest {
        coEvery { deleteAllCards() } returns AppResult.Failure(IllegalStateException("실패"))

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(null) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { CardDeleteState() }
            expectSideEffect(CardDeleteSideEffect.ShredFailure)

            containerHost.onShredTap(null)
            expectState { copy(shredTapCount = 1) }
        }

        coVerify(exactly = 1) { deleteAllCards() }
    }

    @Test
    fun `대상 카드가 있으면 전체가 아니라 그 카드만 삭제한다`() = runTest {
        coEvery { deleteCard(CARD_ID) } returns AppResult.Success(Unit)

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(CARD_ID) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
            expectSideEffect(CardDeleteSideEffect.ShredSuccess)
        }

        coVerify(exactly = 1) { deleteCard(CARD_ID) }
        coVerify(exactly = 0) { deleteAllCards() }
    }

    private fun viewModel() = CardDeleteViewModel(deleteCard, deleteAllCards)

    private companion object {
        const val CARD_ID = 7L
    }
}
