package com.gamss.android.domain.conversation

/**
 * 요청한 방을 하나도 지우지 못했을 때. 원인 하나만 남기면 어느 방이 남았는지 알 수 없어
 * 재시도 대상을 좁힐 수 없으므로 [failures] 로 전부 전한다. 상위에서 캐스팅하지 않아도
 * 로그에서 식별되도록 메시지에도 id 를 싣는다.
 */
class ConversationsDeletionException(
    val failures: List<ConversationDeletionFailure>,
) : Exception(
    "Failed to delete ${failures.size} conversation(s): ${failures.map { it.conversationId }}",
    failures.firstOrNull()?.throwable,
)
