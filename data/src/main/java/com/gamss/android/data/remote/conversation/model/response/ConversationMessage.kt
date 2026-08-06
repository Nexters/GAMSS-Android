package com.gamss.android.data.remote.conversation.model.response

import android.util.Log
import com.gamss.android.data.remote.emotion.toEmotionCharacter
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 대화방 메시지(서버 응답). `senderType` 은 "USER"(사용자) 또는 "CHARACTER"(감정 캐릭터)이고,
 * 캐릭터 메시지의 `emotionType` 은 어떤 캐릭터인지를 나타낸다(예: JOY/ANGER). USER 메시지의
 * emotionType 은 null.
 */
@Serializable
data class ConversationMessage(
    @SerialName("id") val id: Long,
    @SerialName("conversationId") val conversationId: Long,
    @SerialName("senderType") val senderType: String,
    @SerialName("emotionType") val emotionType: String? = null,
    @SerialName("content") val content: String,
    @SerialName("repliesToMessageId") val repliesToMessageId: Long? = null,
    @SerialName("rootMessageId") val rootMessageId: Long? = null,
    @SerialName("createdAt") val createdAt: String,
) {
    companion object {
        const val SENDER_USER = "USER"
        const val SENDER_CHARACTER = "CHARACTER"
    }
}

/**
 * 대화 전체에서 USER 발화 content 만 순서대로 추출한다.
 * 카드 생성(감정 분류·요약)의 입력이 되는 값으로, 캐릭터 반응은 제외한다.
 */
fun List<ConversationMessage>.userUtterances(): List<String> =
    filter { it.senderType == ConversationMessage.SENDER_USER }.map { it.content }

internal fun ConversationMessage.toDomain(): Message? = resolveSender()?.let(::toMessage)

internal fun ConversationMessage.toSentUserMessage(): Message = toMessage(MessageSender.User)

private fun ConversationMessage.toMessage(sender: MessageSender): Message = Message(
    id = id,
    conversationId = conversationId,
    sender = sender,
    content = content,
    repliesToMessageId = repliesToMessageId,
)

private fun ConversationMessage.resolveSender(): MessageSender? = when (senderType) {
    ConversationMessage.SENDER_USER -> MessageSender.User
    ConversationMessage.SENDER_CHARACTER -> emotionType.toEmotionCharacter()?.let(MessageSender::Character)
    else -> {
        Log.w(TAG, "Unknown senderType=$senderType (messageId=$id)")
        null
    }
}

private const val TAG = "ConversationMapper"
