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
    // 저장은 이미 끝났다. 여기서 실패로 접으면 사용자가 재전송해 서버에 중복이 남는다.
    message = message.toSentUserMessage(),
    commentStatus = commentStatus.toCommentGenerationStatus(),
    comments = comments.mapNotNull(ConversationMessage::toDomain),
)

private fun String.toCommentGenerationStatus(): CommentGenerationStatus = when (this) {
    "DONE" -> CommentGenerationStatus.DONE
    "LIMIT_EXCEEDED" -> CommentGenerationStatus.LIMIT_EXCEEDED
    else -> CommentGenerationStatus.FAILED
}
