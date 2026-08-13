package com.gamss.android.feature.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.DeleteConversationsUseCase
import com.gamss.android.domain.conversation.GetOngoingConversationsUseCase
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.SentMessage
import kotlinx.coroutines.CompletableDeferred
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitTestContext
import java.time.LocalDateTime

/**
 * 목록 ViewModel 조립을 한 곳에 둔다. 레포는 페이크로 두고 UseCase 는 실제 구현을 쓴다.
 * 벌크 삭제의 부분 성공 규칙이 UseCase 안에 있어, 그것까지 페이크로 만들면 검증 대상이 사라진다.
 */
internal fun chattingListViewModel(
    repository: ConversationRepository,
): ChattingListViewModel = ChattingListViewModel(
    getOngoingConversations = GetOngoingConversationsUseCase(repository),
    deleteConversations = DeleteConversationsUseCase(repository),
)

/**
 * [ChatRoomTestFakes] 의 페이크와 따로 두는 이유는, 목록 테스트가 조회 결과 교체와 id 별 삭제 실패
 * 주입을 필요로 하기 때문이다. 그쪽 페이크는 조회를 빈 목록으로, 삭제를 항상 성공으로 고정한다.
 */
internal class FakeChattingListRepository(
    conversations: List<Conversation> = emptyList(),
) : ConversationRepository {

    var listResult: AppResult<List<Conversation>> = AppResult.Success(conversations)

    /** id 별 삭제 실패. 여기 없는 id 는 성공한다. */
    var deleteFailures: Map<Long, Throwable> = emptyMap()

    /** 값이 있으면 완료될 때까지 삭제 응답을 붙든다. Deleting 단계를 관찰하기 위한 장치다. */
    var deleteGate: CompletableDeferred<Unit>? = null

    val deletedIds = mutableListOf<Long>()

    var listCallCount = 0
        private set

    override suspend fun getOngoingConversations(): AppResult<List<Conversation>> {
        listCallCount++
        return listResult
    }

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> {
        deleteGate?.await()
        deleteFailures[conversationId]?.let { return AppResult.Failure(it) }
        deletedIds += conversationId
        return AppResult.Success(Unit)
    }

    override suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
    ): AppResult<SentMessage> = error("목록 테스트에서 쓰지 않는다")

    override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
        error("목록 테스트에서 쓰지 않는다")

    override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> =
        error("목록 테스트에서 쓰지 않는다")

    override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
        error("목록 테스트에서 쓰지 않는다")
}

internal fun conversation(
    id: Long,
    title: String? = "대화 $id",
    createdAt: LocalDateTime? = LocalDateTime.of(2026, 7, 31, 4, 20),
): Conversation = Conversation(id = id, title = title, createdAt = createdAt)

internal fun ChattingListViewModel.phase(): ChattingListPhase = container.stateFlow.value.phase

internal fun ChattingListViewModel.rowIds(): List<Long> =
    container.stateFlow.value.groups.flatMap { group -> group.rows.map { it.id } }

/**
 * 상태와 side effect 가 한 스트림으로 오므로 상태를 건너뛰고 다음 side effect 만 받는다.
 * 상태 방출 개수를 세는 방식은 reduce 를 하나 추가할 때마다 관련 없는 테스트가 깨진다.
 */
internal suspend fun ChattingListTestContext.awaitNextSideEffect(): ChattingListSideEffect {
    while (true) {
        val item = awaitItem()
        if (item is Item.SideEffectItem) return item.value
    }
}

internal typealias ChattingListTestContext =
    OrbitTestContext<ChattingListState, ChattingListSideEffect, ChattingListViewModel>
