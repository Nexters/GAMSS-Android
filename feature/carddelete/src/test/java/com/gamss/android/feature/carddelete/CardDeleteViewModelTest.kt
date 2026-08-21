package com.gamss.android.feature.carddelete

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.DeleteCardUseCase
import com.gamss.android.domain.card.DeleteCardsByEmotionUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
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

    private val deleteCardsByEmotion: DeleteCardsByEmotionUseCase = mockk()
    private val deleteCard: DeleteCardUseCase = mockk()

    private fun viewModel() = CardDeleteViewModel(deleteCardsByEmotion, deleteCard)

    @Test
    fun `여섯 번째 탭에서 그 감정 칸을 한 번 비우고 완료 상태가 된다`() = runTest {
        coEvery { deleteCardsByEmotion(EMOTION) } returns AppResult.Success(Unit)

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS - 1) { tapIndex ->
                containerHost.onShredTap(emotion = EMOTION, cardId = null)
                expectState { copy(shredTapCount = tapIndex + 1) }
            }

            containerHost.onShredTap(emotion = EMOTION, cardId = null)
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS) }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
            expectSideEffect(CardDeleteSideEffect.ShredSuccess)
        }

        coVerify(exactly = 1) { deleteCardsByEmotion(EMOTION) }
    }

    /** 다른 감정 칸까지 지워지면 안 된다. 비우기는 들어와 있는 칸 안에서만 일어난다. */
    @Test
    fun `칸을 비울 때 고른 감정만 저장소에 넘긴다`() = runTest {
        coEvery { deleteCardsByEmotion(any()) } returns AppResult.Success(Unit)

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(emotion = EMOTION, cardId = null) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
            expectSideEffect(CardDeleteSideEffect.ShredSuccess)
        }

        coVerify(exactly = 1) { deleteCardsByEmotion(EMOTION) }
        coVerify(exactly = 0) { deleteCardsByEmotion(EmotionCharacter.JOY) }
    }

    @Test
    fun `삭제에 실패하면 초기 상태로 돌아가 다시 시도할 수 있다`() = runTest {
        coEvery { deleteCardsByEmotion(EMOTION) } returns AppResult.Failure(IllegalStateException("실패"))

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(emotion = EMOTION, cardId = null) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { CardDeleteState() }
            expectSideEffect(CardDeleteSideEffect.ShredFailure)

            containerHost.onShredTap(emotion = EMOTION, cardId = null)
            expectState { copy(shredTapCount = 1) }
        }

        coVerify(exactly = 1) { deleteCardsByEmotion(EMOTION) }
    }

    /** 보관함 카드 상세의 "기록 버리기"도 이 화면을 거친다. 그때는 그 카드 한 장만 지워야 한다. */
    @Test
    fun `카드를 지정하면 칸 전체가 아니라 그 카드 한 장만 삭제한다`() = runTest {
        coEvery { deleteCard(CARD_ID) } returns AppResult.Success(Unit)

        viewModel().test(this) {
            repeat(SHRED_TOTAL_TAPS) { containerHost.onShredTap(emotion = EMOTION, cardId = CARD_ID) }
            repeat(SHRED_TOTAL_TAPS) { tapIndex -> expectState { copy(shredTapCount = tapIndex + 1) } }
            expectState { copy(shredTapCount = SHRED_TOTAL_TAPS, isDeleting = true) }
            advanceTimeBy(SHRED_COMPLETE_HOLD_MS)
            expectState { copy(isDeleting = false, isCompleted = true) }
            expectSideEffect(CardDeleteSideEffect.ShredSuccess)
        }

        coVerify(exactly = 1) { deleteCard(CARD_ID) }
        coVerify(exactly = 0) { deleteCardsByEmotion(any()) }
    }

    private companion object {
        const val CARD_ID = 7L
        val EMOTION = EmotionCharacter.ANGER
    }
}
