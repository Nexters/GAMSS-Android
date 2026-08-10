package com.gamss.android.domain.conversation

data class SentMessage(
    val message: Message,
    val commentStatus: CommentGenerationStatus,
    val comments: List<Message>,
)

enum class CommentGenerationStatus {
    DONE,
    FAILED,
    LIMIT_EXCEEDED,
}
