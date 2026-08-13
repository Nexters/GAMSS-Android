package com.gamss.android.domain.conversation

/**
 * @param deletedIds 실제로 지워진 방 id. 입력에 중복이 있었으면 입력보다 짧다.
 * @param failures 지우지 못한 방과 그 원인. 리포지토리 타임아웃이면 [ConversationDeletionFailure.throwable]
 *   이 `CancellationException` 일 수 있다. 다시 던지면 호출자 코루틴이 조용히 끝나므로 던지지 말 것.
 */
data class DeleteConversationsResult(
    val deletedIds: List<Long>,
    val failures: List<ConversationDeletionFailure>,
)

data class ConversationDeletionFailure(
    val conversationId: Long,
    val throwable: Throwable,
)
