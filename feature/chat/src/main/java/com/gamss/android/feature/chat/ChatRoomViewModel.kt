package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.nextCommentRevealGapMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.BreakIterator
import java.util.Locale
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
    private val session: ConversationSession,
) : ViewModel(),
    ContainerHost<ChatRoomState, ChatRoomSideEffect> {

    override val container = container<ChatRoomState, ChatRoomSideEffect>(ChatRoomState())

    private var started = false

    @Volatile
    private var revealJob: Job? = null

    fun start(conversationId: Long?) {
        if (started) return
        started = true
        if (conversationId != null) {
            loadMessages(conversationId)
        }
    }

    private fun loadMessages(conversationId: Long) = intent {
        reduce { state.copy(conversationId = conversationId, isLoading = true) }
        when (val result = session.restore(conversationId)) {
            is AppResult.Success ->
                // 서버 목록엔 댓글이 다 들어 있다. 큐를 남기면 같은 댓글이 두 번 붙어 key 가 충돌한다.
                reduce { state.copy(isLoading = false, messages = result.data, pendingComments = emptyList()) }
            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(LOAD_FAILED))
            }
        }
    }

    fun onInputChange(text: String) = blockingIntent {
        reduce { state.copy(input = text.takeMessageInput(MAX_MESSAGE_LENGTH)) }
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
        flushPendingComments()

        // reduce 는 CAS 재시도로 여러 번 실행되고 마지막 실행만 커밋된다.
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

        val result = session.send(
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
                        messages = state.messages + sent.message + sent.comments.take(1),
                        pendingComments = sent.comments.drop(1),
                        input = if (state.input == sending.content) "" else state.input,
                        replyTarget = state.replyTarget.takeIf { it?.messageId != sending.replyToMessageId },
                    )
                }
                launchCommentReveal()
                sent.commentStatus.toUserMessage()?.let { postSideEffect(ChatRoomSideEffect.ShowToast(it)) }
                session.compact()
            }
            is AppResult.Failure -> {
                reduce { state.copy(isSending = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED))
            }
        }
    }

    fun onEndRequest() = intent {
        // 검사와 상태 전환을 한 reduce 안에서 처리해야 연타로 두 번 시작되지 않는다.
        // 전이 여부는 단계 값이 아니라 별도 플래그로 든다. 진행 중인 단계를 그대로 담으면
        // 이미 CreatingCard 인 상태에서 카드 생성이 한 번 더 시작된다.
        var startCardCreation = false
        reduce {
            startCardCreation = state.canEnd && state.endFlow == EndFlow.CardFailedRetryable
            when {
                !state.canEnd -> state
                // 이미 종료된 뒤 카드만 실패한 경우라 다시 물어볼 게 없다.
                startCardCreation -> state.copy(endFlow = EndFlow.CreatingCard)
                else -> state.copy(endFlow = EndFlow.Confirming)
            }
        }
        if (startCardCreation) runCardCreation()
    }

    fun onEndCancel() = intent {
        reduce {
            if (state.endFlow == EndFlow.Confirming) state.copy(endFlow = EndFlow.NotStarted) else state
        }
    }

    fun onEndConfirm() = intent {
        var accepted = false
        reduce {
            accepted = state.endFlow == EndFlow.Confirming && state.endPreconditionsMet
            when {
                accepted -> state.copy(endFlow = EndFlow.Ending)
                state.endFlow == EndFlow.Confirming -> state.copy(endFlow = EndFlow.NotStarted)
                else -> state
            }
        }
        if (!accepted) return@intent

        // 카드를 만드는 동안 댓글이 하나씩 튀어나오면 어색하다.
        flushPendingComments()

        val ended = session.end(requireNotNull(state.conversationId))
        if (ended is AppResult.Failure) {
            reduce { state.copy(endFlow = EndFlow.NotStarted) }
            postSideEffect(ChatRoomSideEffect.ShowToast(END_FAILED))
            return@intent
        }

        runCardCreation()
    }

    /** 실패는 재시도 가능 여부에 따라 [EndFlow.CardFailedRetryable] 과 [EndFlow.CardFailedFinal] 로 갈린다. */
    private suspend fun ChatRoomSyntax.runCardCreation() {
        reduce { state.copy(endFlow = EndFlow.CreatingCard) }

        val result = session.createCard(requireNotNull(state.conversationId), state.messages)
        val next = when (result) {
            is AppResult.Success -> EndFlow.CardReady(result.data)
            is AppResult.Failure ->
                if (result.throwable is CardNotRetryableException) {
                    EndFlow.CardFailedFinal
                } else {
                    EndFlow.CardFailedRetryable
                }
        }
        reduce { state.copy(endFlow = next) }
        if (result is AppResult.Failure) {
            postSideEffect(ChatRoomSideEffect.ShowToast(result.throwable.toCardFailureMessage()))
        }
    }

    private fun Throwable.toCardFailureMessage(): String = when (this) {
        is CardNotRetryableException.AlreadyExists -> CARD_ALREADY_MADE
        is CardNotRetryableException -> CARD_INPUT_MISSING
        else -> CARD_FAILED
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
        const val END_FAILED = "대화를 끝내지 못했어요"
        const val CARD_FAILED = "카드를 만들지 못했어요. 다시 시도해 주세요."
        const val CARD_ALREADY_MADE = "이 대화의 카드는 이미 만들어졌어요"
        const val CARD_INPUT_MISSING = "카드를 만들 내용이 부족해요"
    }
}

private fun String.takeMessageInput(maxLength: Int): String {
    if (maxLength <= 0) return ""
    if (length <= maxLength) return this

    val endExclusive = BreakIterator.getCharacterInstance(Locale.ROOT).run {
        setText(this@takeMessageInput)
        preceding(maxLength + 1).takeIf { it != BreakIterator.DONE } ?: 0
    }
    return substring(0, endExclusive)
}
