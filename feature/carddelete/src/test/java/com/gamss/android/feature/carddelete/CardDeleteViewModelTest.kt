package com.gamss.android.feature.carddelete

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.DeleteAllCardsUseCase
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

    @Test
    fun `여섯 번째 탭에서 모든 카드를 한 번 삭제하고 완료 상태가 된다`() = runTest {
        coEvery { deleteAllCards() } returns AppResult.Success(Unit)

        CardDeleteViewModel(deleteAllCards).test(this) {
            repeat(SHRED_TOTAL_TAPS - 1) { tapIndex ->
                containerHost.onShredTap()
                expectState { copy(shredTapCount = tapIndex + 1) }
            }

            containerHost.onShredTap()
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

        CardDeleteViewModel(deleteAllCards).test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap() }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { CardDeleteState() }
            expectSideEffect(CardDeleteSideEffect.ShredFailure)

            containerHost.onShredTap()
            expectState { copy(shredTapCount = 1) }
        }

        coVerify(exactly = 1) { deleteAllCards() }
    }
}
