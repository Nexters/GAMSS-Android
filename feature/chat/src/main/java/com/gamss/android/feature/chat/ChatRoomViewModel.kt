package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
) : ViewModel(),
    ContainerHost<ChatRoomState, ChatRoomSideEffect> {

    override val container = container<ChatRoomState, ChatRoomSideEffect>(ChatRoomState())

    private var started = false

    /** 화면 재구성으로 다시 호출돼도 재조회하지 않는다. */
    fun start(conversationId: Long?) {
        if (started) return
        started = true
        if (conversationId != null) {
            loadMessages(conversationId)
        }
    }

    private fun loadMessages(conversationId: Long) = intent {
        reduce { state.copy(conversationId = conversationId, isLoading = true) }
        when (val result = getMessages(conversationId)) {
            is AppResult.Success -> reduce { state.copy(isLoading = false, messages = result.data) }
            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(LOAD_FAILED))
            }
        }
    }

    /** 일반 intent 는 비동기라 글자가 유실된다. runBlocking 이므로 이 블록엔 reduce 만 둔다. */
    fun onInputChange(text: String) = blockingIntent {
        reduce { state.copy(input = text.take(MAX_MESSAGE_LENGTH)) }
    }

    /** 사용자 메시지는 답장 대상이 될 수 없다. */
    fun onReplyTargetSelect(message: Message) = intent {
        val character = (message.sender as? MessageSender.Character)?.character ?: return@intent
        reduce {
            state.copy(
                replyTarget = ReplyTarget(messageId = message.id, characterName = character.displayName),
            )
        }
    }

    fun onReplyTargetClear() = intent {
        reduce { state.copy(replyTarget = null) }
    }

    fun onSend() = intent {
        // 연타 중복 전송을 막으려면 검사와 isSending 설정이 한 reduce 안에 있어야 한다.
        // reduce 는 CAS 재시도로 여러 번 실행되고 마지막 실행만 커밋되므로, 모든 경로에서 덮어쓴다.
        var pending: PendingSend? = null
        reduce {
            pending = if (state.canSend) {
                PendingSend(content = state.input, replyToMessageId = state.replyTarget?.messageId)
            } else {
                null
            }
            if (pending == null) state else state.copy(isSending = true)
        }
        val sending = pending ?: return@intent

        val result = sendMessage(
            conversationId = state.conversationId,
            content = sending.content,
            replyToMessageId = sending.replyToMessageId,
        )

        when (result) {
            is AppResult.Success -> {
                val sent = result.data
                reduce {
                    state.copy(
                        isSending = false,
                        conversationId = sent.message.conversationId,
                        messages = state.messages + sent.message + sent.comments,
                        // 전송하는 동안 새로 입력한 내용은 남긴다.
                        input = if (state.input == sending.content) "" else state.input,
                        replyTarget = state.replyTarget.takeIf { it?.messageId != sending.replyToMessageId },
                    )
                }
                sent.commentStatus.toUserMessage()?.let { postSideEffect(ChatRoomSideEffect.ShowToast(it)) }
            }
            // 실패해도 입력은 지우지 않는다 — 사용자가 쓴 내용을 잃지 않게.
            is AppResult.Failure -> {
                reduce { state.copy(isSending = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED))
            }
        }
    }

    /** 생성이 실패해도 저장은 유지되므로 보낸 메시지를 되돌리지 않고 안내만 띄운다. */
    private fun CommentGenerationStatus.toUserMessage(): String? = when (this) {
        CommentGenerationStatus.DONE -> null
        CommentGenerationStatus.FAILED -> "답장을 받지 못했어요. 잠시 후 다시 보내볼까요?"
        CommentGenerationStatus.LIMIT_EXCEEDED -> "오늘은 대화를 많이 했어요. 내일 다시 이야기해요."
    }

    private data class PendingSend(
        val content: String,
        val replyToMessageId: Long?,
    )

    private companion object {
        const val LOAD_FAILED = "대화를 불러오지 못했어요"
        const val SEND_FAILED = "메시지를 보내지 못했어요"
    }
}
