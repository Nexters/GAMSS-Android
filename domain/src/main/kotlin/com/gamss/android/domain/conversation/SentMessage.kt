package com.gamss.android.domain.conversation

data class SentMessage(
    val message: Message,
    val commentStatus: CommentGenerationStatus,
    /** [CommentGenerationStatus.DONE] 이 아니면 빈 리스트. */
    val comments: List<Message>,
)

/** DONE 이 아니어도 메시지 저장은 유지되므로 보낸 메시지를 되돌리면 안 된다. */
enum class CommentGenerationStatus {
    DONE,
    FAILED,
    LIMIT_EXCEEDED,
}
