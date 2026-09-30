package com.gamss.android.feature.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
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
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChattingListLoadTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 빈_목록에서_네트워크로_실패하면_토스트_없이_네트워크_에러가_된다() = runTest {
        val repository = FakeChattingListRepository().apply { listResult = networkFailure() }

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            assertEquals(ChattingListPhase.NetworkError, awaitPhase())
            expectNoItems()
        }
    }

    @Test
    fun 목록이_보이는_중에_네트워크로_실패하면_목록을_두고_토스트만_띄운다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            repository.listResult = networkFailure()
            containerHost.load()
            runCurrent()

            assertEquals(ChattingListSideEffect.ShowLoadFailed, awaitNextSideEffect())
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertEquals(listOf(1L), containerHost.rowIds())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 네트워크_에러에서_다시_불러오면_로딩을_거치지_않고_목록이_된다() = runTest {
        val repository = FakeChattingListRepository().apply { listResult = networkFailure() }

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()
            assertEquals(ChattingListPhase.NetworkError, awaitPhase())

            repository.listResult = AppResult.Success(listOf(conversation(1L)))
            containerHost.load()
            runCurrent()

            assertEquals(ChattingListPhase.Browsing, awaitPhase())
            assertEquals(listOf(1L), containerHost.rowIds())
            expectNoItems()
        }
    }

    @Test
    fun 네트워크가_아닌_실패는_빈_목록과_토스트를_보여준다() = runTest {
        val repository = FakeChattingListRepository().apply {
            listResult = AppResult.Failure(IllegalStateException("list failed"))
        }

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            assertEquals(ChattingListSideEffect.ShowLoadFailed, awaitNextSideEffect())
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 네트워크_에러에서_다시_불러와도_네트워크로_실패하면_에러_화면을_그대로_둔다() = runTest {
        val repository = FakeChattingListRepository().apply { listResult = networkFailure() }

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()
            assertEquals(ChattingListPhase.NetworkError, awaitPhase())

            containerHost.load()
            runCurrent()

            expectNoItems()
        }
    }

    @Test
    fun 불러온_목록이_비어_있으면_다시_불러오다_네트워크로_실패해도_빈_목록과_토스트를_보여준다() = runTest {
        val repository = FakeChattingListRepository()

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            repository.listResult = networkFailure()
            containerHost.load()
            runCurrent()

            assertEquals(ChattingListSideEffect.ShowLoadFailed, awaitNextSideEffect())
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 네트워크_에러_중에_카드를_누르면_채팅방을_연다() = runTest {
        val repository = FakeChattingListRepository().apply { listResult = networkFailure() }

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            containerHost.onCardClick(1L)
            runCurrent()

            assertEquals(ChattingListSideEffect.OpenChatRoom(1L), awaitNextSideEffect())
            assertEquals(ChattingListPhase.NetworkError, containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 조회_중에_다시_불러도_요청은_한_번만_나간다() = runTest {
        val repository = FakeChattingListRepository().apply { listResult = networkFailure() }

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()
            assertEquals(ChattingListPhase.NetworkError, containerHost.phase())

            val callsBefore = repository.listCallCount
            val gate = CompletableDeferred<Unit>()
            repository.listGate = gate
            repository.listResult = AppResult.Success(listOf(conversation(1L)))
            containerHost.load()
            containerHost.load()
            runCurrent()
            assertEquals(1, repository.listCallCount - callsBefore)

            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertEquals(listOf(1L), containerHost.rowIds())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 조회_중에_선택_모드에_들어가면_응답이_와도_선택이_유지된다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            repository.listGate = gate
            containerHost.load()
            runCurrent()
            containerHost.onCardLongClick(1L)
            runCurrent()

            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 조회_중에_고른_방이_새_목록에서_사라지면_선택에서_빠진다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            repository.listGate = gate
            repository.listResult = AppResult.Success(listOf(conversation(1L)))
            containerHost.load()
            runCurrent()
            containerHost.onCardLongClick(1L)
            containerHost.onCardLongClick(2L)
            runCurrent()

            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())
            assertEquals(listOf(1L), containerHost.rowIds())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 조회_중에_선택_모드에_들어갔는데_새_목록이_비면_선택_모드를_나간다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            repository.listGate = gate
            repository.listResult = AppResult.Success(emptyList())
            containerHost.load()
            runCurrent()
            containerHost.onCardLongClick(1L)
            runCurrent()

            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListPhase.Browsing, containerHost.phase())
            assertEquals(emptyList<Long>(), containerHost.rowIds())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 조회_중에_선택_모드에_들어가면_조회가_실패해도_선택이_유지된다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            repository.listGate = gate
            repository.listResult = networkFailure()
            containerHost.load()
            runCurrent()
            containerHost.onCardLongClick(1L)
            runCurrent()

            gate.complete(Unit)
            runCurrent()
            assertEquals(ChattingListSideEffect.ShowLoadFailed, awaitNextSideEffect())
            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 삭제_전에_보낸_조회가_늦게_와도_지운_방은_되살아나지_않는다() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L), conversation(2L)))

        chattingListViewModel(repository).test(this) {
            containerHost.load()
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            repository.listGate = gate
            containerHost.load()
            runCurrent()

            repository.listGate = null
            repository.listResult = AppResult.Success(listOf(conversation(2L)))
            containerHost.onCardLongClick(1L)
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            gate.complete(Unit)
            runCurrent()
            assertEquals(listOf(1L), repository.deletedIds)
            assertEquals(listOf(2L), containerHost.rowIds())

            cancelAndIgnoreRemainingItems()
        }
    }

    private suspend fun ChattingListTestContext.awaitPhase(): ChattingListPhase =
        (awaitItem() as Item.StateItem).value.phase

    private fun networkFailure() = AppResult.Failure(ApiException.Network(IOException("offline")))
}
