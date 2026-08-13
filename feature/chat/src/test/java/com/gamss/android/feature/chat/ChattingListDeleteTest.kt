package com.gamss.android.feature.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.SessionExpiredException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.test

/** 벌크 삭제 결과별 화면 반응. 부분 실패와 전체 실패의 비대칭이 핵심이다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChattingListDeleteTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 모두_지워지면_목록을_다시_조회하고_선택_모드를_나간다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()
            val callsAfterLoad = repository.listCallCount

            containerHost.onCardLongClick(1L)
            containerHost.onCardLongClick(2L)
            runCurrent()

            repository.listResult = AppResult.Success(emptyList())
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(setOf(1L, 2L), repository.deletedIds.toSet())
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertEquals(emptyList<Long>(), containerHost.rowIds())
            assertEquals(callsAfterLoad + 1, repository.listCallCount)
            assertEquals(ChattingListSideEffect.ShowDeleteSucceeded, awaitNextSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 일부만_실패하면_선택_모드를_나가고_실패_개수를_알린다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))
        repository.deleteFailures = mapOf(2L to IllegalStateException("delete failed"))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            containerHost.onCardLongClick(2L)
            runCurrent()

            repository.listResult = AppResult.Success(listOf(conversation(2L)))
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(listOf(1L), repository.deletedIds)
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertEquals(listOf(2L), containerHost.rowIds())
            assertEquals(ChattingListSideEffect.ShowDeletePartiallyFailed(failedCount = 1), awaitNextSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 하나도_못_지우면_선택을_유지하고_재조회하지_않는다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))
        repository.deleteFailures = mapOf(
            1L to IllegalStateException("delete failed"),
            2L to IllegalStateException("delete failed"),
        )

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()
            val callsAfterLoad = repository.listCallCount

            containerHost.onCardLongClick(1L)
            containerHost.onCardLongClick(2L)
            runCurrent()

            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(setOf(1L, 2L)), containerHost.phase())
            assertEquals(listOf(1L, 2L), containerHost.rowIds())
            assertEquals(callsAfterLoad, repository.listCallCount)
            assertEquals(ChattingListSideEffect.ShowDeleteFailed, awaitNextSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 세션이_만료되면_만료만_알리고_조회_실패는_알리지_않는다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))
        repository.deleteFailures = mapOf(1L to SessionExpiredException())

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            // 만료된 세션으로 다시 조회해도 같은 오류가 온다.
            repository.listResult = AppResult.Failure(SessionExpiredException())
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertEquals(ChattingListSideEffect.ShowSessionExpired, awaitNextSideEffect())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 재조회가_실패하면_지운_방만_로컬에서_지운다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            repository.listResult = AppResult.Failure(IllegalStateException("list failed"))
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(listOf(2L), containerHost.rowIds())
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())

            // 삭제 성공만 알린다. 조회 실패까지 알리면 두 토스트가 서로 모순되게 읽힌다.
            assertEquals(ChattingListSideEffect.ShowDeleteSucceeded, awaitNextSideEffect())
            assertTrue(awaitItem() is Item.StateItem)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 삭제가_도는_중_load_가_다시_불려도_단계를_되돌리지_않는다() = runTest {
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

            containerHost.load()
            runCurrent()
            assertEquals(ChattingListPhase.Deleting(setOf(1L)), containerHost.phase())

            repository.deleteGate = null
            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }
}
