package com.gamss.android.data.remote.conversation.model.response

import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.SentMessage
import kotlinx.serialization.Serializable

@Serializable
internal data class SaveMessageResponse(
    val message: ConversationMessage,
    val commentStatus: String,
    val comments: List<ConversationMessage> = emptyList(),
)

internal fun SaveMessageResponse.toDomain(): SentMessage = SentMessage(
    message = message.toSentUserMessage(),
    commentStatus = commentStatus.toCommentGenerationStatus(),
    comments = comments.mapNotNull(ConversationMessage::toDomain),
)

private fun String.toCommentGenerationStatus(): CommentGenerationStatus = when (this) {
    "DONE" -> CommentGenerationStatus.DONE
    "LIMIT_EXCEEDED" -> CommentGenerationStatus.LIMIT_EXCEEDED
    else -> CommentGenerationStatus.FAILED
}
