package com.gamss.android.feature.carddelete

import com.gamss.android.core.common.AppResult
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

    private val deleteCard: DeleteCardUseCase = mockk()

    @Test
    fun `여섯 번째 탭에서 전달받은 카드만 한 번 삭제하고 완료 상태가 된다`() = runTest {
        coEvery { deleteCard(CARD_ID) } returns AppResult.Success(Unit)

        CardDeleteViewModel(deleteCard).test(this) {
            repeat(SHRED_TOTAL_TAPS - 1) { tapIndex ->
                containerHost.onShredTap(CARD_ID)
                expectState { copy(shredTapCount = tapIndex + 1) }
            }

            containerHost.onShredTap(CARD_ID)
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS) }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
            expectSideEffect(CardDeleteSideEffect.ShredSuccess)
        }

        coVerify(exactly = 1) { deleteCard(CARD_ID) }
    }

    @Test
    fun `삭제에 실패하면 초기 상태로 돌아가 다시 시도할 수 있다`() = runTest {
        coEvery { deleteCard(CARD_ID) } returns AppResult.Failure(IllegalStateException("실패"))

        CardDeleteViewModel(deleteCard).test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(CARD_ID) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { CardDeleteState() }
            expectSideEffect(CardDeleteSideEffect.ShredFailure)

            containerHost.onShredTap(CARD_ID)
            expectState { copy(shredTapCount = 1) }
        }

        coVerify(exactly = 1) { deleteCard(CARD_ID) }
    }

    @Test
    fun `애니메이션 미리보기는 삭제 요청 없이 완료 상태를 보여준다`() = runTest {
        CardDeleteViewModel(deleteCard).test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(CARD_ID, isAnimationPreview = true) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
        }

        coVerify(exactly = 0) { deleteCard(any()) }
    }

    private companion object {
        const val CARD_ID = 42L
    }
}
