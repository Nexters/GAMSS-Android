package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.SessionExpiredException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteConversationsUseCaseTest {

    @Test
    fun 빈_리스트면_서버를_부르지_않고_성공한다() = runTest {
        val repository = RecordingRepository()

        val result = DeleteConversationsUseCase(repository)(emptyList())

        assertTrue(repository.deletedIds.isEmpty())
        val data = (result as AppResult.Success).data
        assertTrue(data.deletedIds.isEmpty())
        assertTrue(data.failures.isEmpty())
    }

    @Test
    fun 중복_id는_한_번만_지운다() = runTest {
        val repository = RecordingRepository()

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 1L, 2L, 1L))

        assertEquals(listOf(1L, 2L), repository.deletedIds)
        assertEquals(listOf(1L, 2L), (result as AppResult.Success).data.deletedIds)
    }

    @Test
    fun 전부_성공하면_입력_순서대로_삭제된_id를_돌려준다() = runTest {
        val repository = RecordingRepository()

        val result = DeleteConversationsUseCase(repository)(listOf(3L, 1L, 2L))

        assertEquals(listOf(3L, 1L, 2L), (result as AppResult.Success).data.deletedIds)
    }

    @Test
    fun 일부만_실패하면_성공과_실패를_갈라서_성공으로_돌려준다() = runTest {
        val repository = RecordingRepository(failingIds = setOf(2L))

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 2L, 3L))

        val data = (result as AppResult.Success).data
        assertEquals(listOf(1L, 3L), data.deletedIds)
        assertEquals(listOf(2L), data.failures.map { it.conversationId })
    }

    @Test
    fun 전부_실패하면_실패한_id를_모두_담아_실패한다() = runTest {
        val repository = RecordingRepository(failingIds = setOf(1L, 2L))

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 2L))

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ConversationsDeletionException)
        assertEquals(listOf(1L, 2L), (throwable as ConversationsDeletionException).failures.map { it.conversationId })
    }

    /** 같은 예외 인스턴스를 여러 id 가 공유하는 건 흔하다. addSuppressed 로 합치면 self-suppression 으로 죽는다. */
    @Test
    fun 같은_예외_인스턴스가_여러_id에서_나와도_죽지_않는다() = runTest {
        val shared = IllegalStateException("delete failed")
        val repository = RecordingRepository(failure = { AppResult.Failure(shared) }, failingIds = setOf(1L, 2L, 3L))

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 2L, 3L))

        assertEquals(3, ((result as AppResult.Failure).throwable as ConversationsDeletionException).failures.size)
    }

    @Test
    fun 리포지토리가_예외를_던져도_그_id만_실패로_담는다() = runTest {
        val repository = RecordingRepository(throwingIds = setOf(2L))

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 2L, 3L))

        val data = (result as AppResult.Success).data
        assertEquals(listOf(1L, 3L), data.deletedIds)
        assertEquals(listOf(2L), data.failures.map { it.conversationId })
    }

    @Test
    fun 한_건이_예외를_던져도_나머지_삭제는_계속된다() = runTest {
        val repository = RecordingRepository(throwingIds = setOf(1L))

        DeleteConversationsUseCase(repository)(listOf(1L, 2L, 3L))

        assertEquals(listOf(1L, 2L, 3L), repository.deletedIds)
    }

    /**
     * 리포지토리 자체 타임아웃은 CancellationException 이라 그대로 올리면 배치 전체가 취소되고
     * 이미 지운 방 목록까지 사라진다. 남의 취소는 그 id 하나의 실패여야 한다.
     */
    @Test
    fun 리포지토리_한_건이_타임아웃돼도_나머지는_지운다() = runTest {
        val repository = RecordingRepository(timingOutIds = setOf(2L))

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 2L, 3L))

        val data = (result as AppResult.Success).data
        assertEquals(listOf(1L, 3L), data.deletedIds)
        assertEquals(listOf(2L), data.failures.map { it.conversationId })
    }

    /** 세션 만료가 부분 성공에 묻히면 호출부가 재로그인 시점을 놓친다. */
    @Test
    fun 세션이_만료되면_부분_성공이_아니라_실패로_낸다() = runTest {
        val repository = RecordingRepository(
            failure = { AppResult.Failure(SessionExpiredException()) },
            failingIds = setOf(2L),
        )

        val result = DeleteConversationsUseCase(repository)(listOf(1L, 2L, 3L))

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    /**
     * 페이크가 반드시 suspend 해야 의미가 있다. 곧바로 반환하면 단일 스레드 스케줄러에서
     * 동시 진행 수가 늘 1이라 세마포어를 지워도 통과한다.
     */
    @Test
    fun 동시에_상한만큼만_호출한다() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = RecordingRepository(gate = gate)
        val ids = (1L..10L).toList()

        val caller = launch { DeleteConversationsUseCase(repository)(ids) }
        advanceUntilIdle()
        val peak = repository.peakInFlight
        gate.complete(Unit)
        caller.join()

        assertEquals(MAX_CONCURRENT_DELETES, peak)
        assertEquals(ids, repository.deletedIds)
    }

    @Test
    fun 취소되면_상한을_넘겨_새_삭제를_시작하지_않는다() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = RecordingRepository(gate = gate)

        val caller = launch { DeleteConversationsUseCase(repository)((1L..10L).toList()) }
        advanceUntilIdle()
        caller.cancel()
        gate.complete(Unit)
        caller.join()

        assertEquals(MAX_CONCURRENT_DELETES, repository.deletedIds.size)
    }

    private class RecordingRepository(
        private val failingIds: Set<Long> = emptySet(),
        private val throwingIds: Set<Long> = emptySet(),
        private val timingOutIds: Set<Long> = emptySet(),
        private val failure: () -> AppResult.Failure = { AppResult.Failure(IllegalStateException("delete failed")) },
        private val gate: CompletableDeferred<Unit>? = null,
    ) : ConversationRepository {
        private val recorded = mutableListOf<Long>()
        private var inFlight = 0

        val deletedIds: List<Long> get() = recorded
        var peakInFlight = 0
            private set

        override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> {
            recorded += conversationId
            inFlight++
            peakInFlight = maxOf(peakInFlight, inFlight)
            try {
                gate?.await()
                if (conversationId in timingOutIds) withTimeout(TIMEOUT_MILLIS) { delay(TIMEOUT_MILLIS * 2) }
                check(conversationId !in throwingIds) { "delete exploded" }
                return if (conversationId in failingIds) failure() else AppResult.Success(Unit)
            } finally {
                inFlight--
            }
        }

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
        ): AppResult<SentMessage> = throw UnsupportedOperationException()

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            throw UnsupportedOperationException()

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
            throw UnsupportedOperationException()

        private companion object {
            const val TIMEOUT_MILLIS = 10L
        }
    }
}
