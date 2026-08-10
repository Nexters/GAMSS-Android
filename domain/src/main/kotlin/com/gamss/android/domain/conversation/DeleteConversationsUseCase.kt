package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.SessionExpiredException
import com.gamss.android.domain.usecase.UseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

/** 서버에 한꺼번에 여는 삭제 요청 수. 방을 수십 개 골라도 서버가 한 번에 받는 양은 이만큼으로 묶인다. */
private const val MAX_CONCURRENT_DELETES = 4

/**
 * 방 여러 개를 지운다. 벌크 엔드포인트가 없어 id 하나당 한 번씩 서버를 부른다.
 *
 * 일부만 실패해도 성공한 삭제는 되돌릴 수 없으므로, 성공·실패를 갈라 [DeleteConversationsResult] 로 돌려준다.
 * 하나도 지우지 못했을 때만 실패다.
 *
 * 취소되면 이미 지워진 방이 있어도 호출자는 그 목록을 받지 못한다. 취소 후에는 목록을 다시 조회해야 한다.
 */
class DeleteConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<List<Long>, AppResult<DeleteConversationsResult>> {

    override suspend fun invoke(params: List<Long>): AppResult<DeleteConversationsResult> {
        val ids = params.distinct()
        // 호출마다 새로 만든다. 프로퍼티로 두면 상한이 호출 단위도 앱 단위도 아닌 값이 된다.
        val semaphore = Semaphore(MAX_CONCURRENT_DELETES)

        val outcomes = coroutineScope {
            ids.map { id ->
                async { id to deleteCatching(semaphore, id) }
            }.awaitAll()
        }

        val deletedIds = outcomes.filter { it.second is AppResult.Success }.map { it.first }
        val failures = outcomes.mapNotNull { (id, result) ->
            (result as? AppResult.Failure)?.let { ConversationDeletionFailure(id, it.throwable) }
        }

        val sessionExpired = failures.map { it.throwable }.filterIsInstance<SessionExpiredException>().firstOrNull()
        return when {
            // 세션이 끊겼다는 사실이 부분 성공 안에 묻히면 호출부가 재로그인 시점을 놓친다.
            sessionExpired != null -> AppResult.Failure(sessionExpired)
            deletedIds.isEmpty() && failures.isNotEmpty() ->
                AppResult.Failure(ConversationsDeletionException(failures))

            else -> AppResult.Success(DeleteConversationsResult(deletedIds, failures))
        }
    }

    /**
     * 한 건의 실패가 형제 코루틴을 취소시키면, 이미 지운 방 목록이 통째로 사라지고 이미 나간 요청도 끊긴다.
     * 그래서 취소를 제외한 모든 예외를 값으로 바꾼다.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun deleteCatching(semaphore: Semaphore, id: Long): AppResult<Unit> =
        try {
            semaphore.withPermit { conversationRepository.deleteConversation(id) }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            AppResult.Failure(t)
        }
}
