package com.gamss.android.domain.conversation

/**
 * 요청한 방을 하나도 지우지 못했을 때. 어떤 방이 왜 실패했는지 [failures] 로 전한다.
 * 원인 하나만 남기면 어느 방이 남았는지 알 수 없어 재시도 대상을 좁힐 수 없다.
 */
class ConversationsDeletionException(
    val failures: List<ConversationDeletionFailure>,
) : Exception(
    "Failed to delete ${failures.size} conversation(s)",
    failures.firstOrNull()?.throwable,
)
