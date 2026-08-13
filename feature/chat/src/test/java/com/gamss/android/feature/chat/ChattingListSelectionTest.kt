package com.gamss.android.feature.chat

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

/** 선택 모드 전이. 카드 탭의 의미가 단계에 따라 달라지는 것이 이 화면의 핵심 규칙이다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChattingListSelectionTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 길게_누르면_그_방만_선택된다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 같은_방을_다시_길게_눌러도_선택은_그대로다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onCardLongClick(1L)
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 선택_모드의_탭은_토글이고_방을_열지_않는다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onCardClick(2L)
            runCurrent()
            assertEquals(ChattingListPhase.Selecting(setOf(1L, 2L)), containerHost.phase())

            containerHost.onCardClick(1L)
            runCurrent()
            assertEquals(ChattingListPhase.Selecting(setOf(2L)), containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 목록을_보는_중_탭은_방을_연다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardClick(2L)
            runCurrent()

            assertEquals(ChattingListSideEffect.OpenChatRoom(2L), awaitNextSideEffect())
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 삭제_액션은_아무것도_고르지_않은_선택_모드로_진입시킨다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onDeleteActionClick()
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(emptySet()), containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 아무것도_고르지_않았으면_하단_삭제_버튼이_아무_일도_하지_않는다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))
        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onDeleteActionClick()
            containerHost.onDeleteRequest()
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(emptySet()), containerHost.phase())
            assertTrue(repository.deletedIds.isEmpty())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 하단_삭제_버튼은_바로_지우지_않고_확인_단계로_넘긴다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))
        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onDeleteRequest()
            runCurrent()

            assertEquals(ChattingListPhase.Confirming(setOf(1L)), containerHost.phase())
            assertTrue(repository.deletedIds.isEmpty())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 확인을_물르면_고른_방을_유지한_채_선택_모드로_돌아온다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))
        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onCardLongClick(2L)
            containerHost.onDeleteRequest()
            runCurrent()

            containerHost.onDeleteDismiss()
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(setOf(1L, 2L)), containerHost.phase())
            assertTrue(repository.deletedIds.isEmpty())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 확인_중에는_탭과_길게_누르기가_선택을_바꾸지_않는다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onDeleteRequest()
            runCurrent()

            containerHost.onCardClick(2L)
            containerHost.onCardLongClick(2L)
            runCurrent()

            assertEquals(ChattingListPhase.Confirming(setOf(1L)), containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 마지막_선택을_해제해도_선택_모드에_남는다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onCardClick(1L)
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(emptySet()), containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 뒤로가기는_선택_모드만_빠져나간다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()
            assertTrue(containerHost.container.stateFlow.value.canCancelSelection)

            containerHost.onSelectionCancel()
            runCurrent()

            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertFalse(containerHost.container.stateFlow.value.canCancelSelection)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 로딩_중에는_길게_눌러도_선택되지_않는다() = runTest {
        viewModel().test(this) {
            // load 를 부르지 않아 초기 단계인 Loading 에 머문다.
            containerHost.onCardLongClick(1L)
            runCurrent()

            assertEquals(ChattingListPhase.Loading, containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 화면이_다시_구성돼_load_가_불려도_선택이_지워지지_않는다() = runTest {
        viewModel().test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            // 회전이나 탭 재진입으로 LaunchedEffect 가 다시 도는 상황.
            containerHost.load()
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 삭제가_도는_중에는_탭과_길게_누르기와_취소를_모두_무시한다() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            repository.deleteGate = gate
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()
            assertEquals(ChattingListPhase.Deleting(setOf(1L)), containerHost.phase())

            containerHost.onCardLongClick(2L)
            containerHost.onCardClick(2L)
            containerHost.onSelectionCancel()
            runCurrent()
            assertEquals(ChattingListPhase.Deleting(setOf(1L)), containerHost.phase())

            repository.deleteGate = null
            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 삭제가_도는_중_삭제_액션을_다시_눌러도_한_번만_요청한다() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            repository.deleteGate = gate
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            repository.deleteGate = null
            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(1L), repository.deletedIds)
            cancelAndIgnoreRemainingItems()
        }
    }

    private fun viewModel(): ChattingListViewModel = chattingListViewModel(
        FakeChattingListRepository(listOf(conversation(1L), conversation(2L))),
    )
}
