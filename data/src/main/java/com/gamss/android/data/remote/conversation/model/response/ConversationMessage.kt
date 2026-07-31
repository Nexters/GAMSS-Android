package com.gamss.android.data.remote.conversation.model.response

import android.util.Log
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
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

/** 발신 주체를 해석할 수 없으면 잘못된 주체로 그리는 대신 null 을 돌려 목록에서 제외한다. */
internal fun ConversationMessage.toDomain(): Message? = resolveSender()?.let(::toMessage)

/** 내가 보낸 게 확실한 메시지. 발신 주체 해석 실패와 무관하게 화면에 남는다. */
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

/** 서버 문자열에 기대는 코드는 이 함수에만 둔다. */
private fun String?.toEmotionCharacter(): EmotionCharacter? = when (this) {
    "JOY" -> EmotionCharacter.JOY
    "ANGER" -> EmotionCharacter.ANGER
    "ANXIETY" -> EmotionCharacter.ANXIETY
    "GRUMPY" -> EmotionCharacter.PRICKLY
    "WARM" -> EmotionCharacter.WARM
    "QUIRKY" -> EmotionCharacter.QUIRKY
    else -> {
        Log.w(TAG, "Unknown emotionType=$this")
        null
    }
}

private const val TAG = "ConversationMapper"
