package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.nextCommentRevealGapMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

private typealias ChatRoomSyntax = Syntax<ChatRoomState, ChatRoomSideEffect>

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
    private val summaryStore: ConversationSummaryStore,
) : ViewModel(),
    ContainerHost<ChatRoomState, ChatRoomSideEffect> {

    override val container = container<ChatRoomState, ChatRoomSideEffect>(ChatRoomState())

    private var started = false

    /** 네트워크 응답 스레드에서 쓰고 이벤트 루프 스레드에서 읽으므로 @Volatile 이 필요하다. */
    @Volatile
    private var revealJob: Job? = null

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
            is AppResult.Success -> {
                // 서버 목록엔 댓글이 다 들어 있다. 큐를 남기면 같은 댓글이 두 번 붙어 key 가 충돌한다.
                reduce { state.copy(isLoading = false, messages = result.data, pendingComments = emptyList()) }
                summaryStore.restore(result.data.userUtterances())
            }
            is AppResult.Failure -> {
                // 이전 대화가 남아 있으면 다음 전송의 압축본에 섞인다.
                summaryStore.reset()
                reduce { state.copy(isLoading = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(LOAD_FAILED))
            }
        }
    }

    /** 일반 intent 는 비동기라 글자가 유실된다. runBlocking 이므로 이 블록엔 reduce 만 둔다. */
    fun onInputChange(text: String) = blockingIntent {
        reduce { state.copy(input = text.take(MAX_MESSAGE_LENGTH)) }
    }

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
        // 앞선 노출이 남아 있으면 순서가 뒤엉키므로 먼저 다 붙이고 시작한다.
        flushPendingComments()

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
            SendMessageUseCase.Params(
                conversationId = state.conversationId,
                content = sending.content,
                replyToMessageId = sending.replyToMessageId,
                contextSummary = summaryStore.currentContextSummary(),
            ),
        )

        when (result) {
            is AppResult.Success -> {
                val sent = result.data
                reduce {
                    state.copy(
                        isSending = false,
                        conversationId = sent.message.conversationId,
                        messages = state.messages + sent.message + sent.comments.take(1),
                        pendingComments = sent.comments.drop(1),
                        // 전송하는 동안 새로 입력한 내용은 남긴다.
                        input = if (state.input == sending.content) "" else state.input,
                        replyTarget = state.replyTarget.takeIf { it?.messageId != sending.replyToMessageId },
                    )
                }
                launchCommentReveal()
                sent.commentStatus.toUserMessage()?.let { postSideEffect(ChatRoomSideEffect.ShowToast(it)) }
                // 요약기가 돌 수 있어 화면 갱신 뒤에 둔다.
                summaryStore.add(sent.message.content)
            }
            is AppResult.Failure -> {
                reduce { state.copy(isSending = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED))
            }
        }
    }

    private fun launchCommentReveal() {
        revealJob = intent {
            while (state.pendingComments.isNotEmpty()) {
                delay(nextCommentRevealGapMillis())
                reduce {
                    val next = state.pendingComments.firstOrNull()
                    if (next == null) {
                        state
                    } else {
                        state.copy(
                            messages = state.messages + next,
                            pendingComments = state.pendingComments.drop(1),
                        )
                    }
                }
            }
        }
    }

    /** Orbit 의 subIntent 가 같은 역할이지만 @OrbitExperimental 이라 Syntax 확장으로 둔다. */
    private suspend fun ChatRoomSyntax.flushPendingComments() {
        revealJob?.cancelAndJoin()
        revealJob = null
        reduce {
            if (state.pendingComments.isEmpty()) {
                state
            } else {
                state.copy(
                    messages = state.messages + state.pendingComments,
                    pendingComments = emptyList(),
                )
            }
        }
    }

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

private fun List<Message>.userUtterances(): List<String> =
    filter { it.sender == MessageSender.User }.map { it.content }
