package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.getOrNull
import com.gamss.android.domain.card.CardAlreadyExistsException
import com.gamss.android.domain.card.CreateCardUseCase
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.CommentRevealPolicy
import com.gamss.android.domain.conversation.EndConversationUseCase
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
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
    private val endConversation: EndConversationUseCase,
    private val summarizeDiary: SummarizeDiaryUseCase,
    private val createCard: CreateCardUseCase,
    private val emotionAccumulator: ConversationEmotionAccumulator,
) : ViewModel(),
    ContainerHost<ChatRoomState, ChatRoomSideEffect> {

    override val container = container<ChatRoomState, ChatRoomSideEffect>(ChatRoomState())

    private var started = false

    /**
     * 노출 중인 코루틴. 새 전송이나 종료가 오면 취소하고 남은 댓글을 즉시 붙인다.
     * 네트워크 응답 스레드에서 쓰고 이벤트 루프 스레드에서 읽으므로 @Volatile 이 필요하다.
     */
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
                // 배치와 증분 결과가 같으므로 복원 시 순서대로 다시 누적하면 된다.
                emotionAccumulator.reset()
                emotionAccumulator.addAll(result.data.userContents())
                // 서버 목록엔 댓글이 다 들어 있다. 큐를 남기면 같은 댓글이 두 번 붙어 key 가 충돌한다.
                reduce { state.copy(isLoading = false, messages = result.data, pendingComments = emptyList()) }
            }
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
            conversationId = state.conversationId,
            content = sending.content,
            replyToMessageId = sending.replyToMessageId,
        )

        when (result) {
            is AppResult.Success -> {
                val sent = result.data
                // isSending 이 풀리기 전에 누적해야 canEnd 가 누적기를 보호한다.
                emotionAccumulator.add(sent.message.content)
                reduce {
                    state.copy(
                        isSending = false,
                        conversationId = sent.message.conversationId,
                        // 첫 댓글은 서버 왕복이 대기 시간이라 바로 붙이고, 나머지는 간격을 두고 노출한다.
                        messages = state.messages + sent.message + sent.comments.take(1),
                        pendingComments = sent.comments.drop(1),
                        // 전송하는 동안 새로 입력한 내용은 남긴다.
                        input = if (state.input == sending.content) "" else state.input,
                        replyTarget = state.replyTarget.takeIf { it?.messageId != sending.replyToMessageId },
                    )
                }
                startRevealing()
                sent.commentStatus.toUserMessage()?.let { postSideEffect(ChatRoomSideEffect.ShowToast(it)) }
            }
            // 실패해도 입력은 지우지 않는다. 사용자가 쓴 내용을 잃지 않게.
            is AppResult.Failure -> {
                reduce { state.copy(isSending = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED))
            }
        }
    }

    fun onEndRequest() = intent {
        // 검사와 상태 전환을 한 reduce 안에서 처리해야 연타로 두 번 시작되지 않는다.
        var retryCard = false
        reduce {
            retryCard = state.canEnd && state.isEnded
            when {
                // 이미 종료된 뒤 카드만 실패한 경우라 다시 물어볼 게 없다.
                retryCard -> state.copy(isFinishing = true)
                state.canEnd -> state.copy(showEndConfirm = true)
                else -> state
            }
        }
        if (retryCard) runCardCreation()
    }

    fun onEndCancel() = intent {
        reduce { state.copy(showEndConfirm = false) }
    }

    fun onCardDismiss() = intent {
        reduce { state.copy(card = null) }
    }

    /** 감정은 전송할 때마다 누적해 둔 값이라 여기서 추론하지 않는다. */
    fun onEndConfirm() = intent {
        var accepted = false
        reduce {
            accepted = state.canEnd
            if (accepted) {
                state.copy(showEndConfirm = false, isFinishing = true)
            } else {
                state.copy(showEndConfirm = false)
            }
        }
        if (!accepted) return@intent

        // 카드를 만드는 동안 댓글이 하나씩 튀어나오면 어색하다.
        flushPendingComments()

        val ended = endConversation(requireNotNull(state.conversationId))
        if (ended is AppResult.Failure) {
            reduce { state.copy(isFinishing = false) }
            postSideEffect(ChatRoomSideEffect.ShowToast(END_FAILED))
            return@intent
        }
        reduce { state.copy(isEnded = true) }

        runCardCreation()
    }

    /** 실패하면 [ChatRoomState.endedButCardFailed] 로 재시도 경로를 남긴다. */
    private suspend fun ChatRoomSyntax.runCardCreation() {
        reduce { state.copy(isFinishing = true, endedButCardFailed = false) }

        val conversationId = state.conversationId
        val emotion = emotionAccumulator.result()
        val summary = summarizeDiary(state.messages.userContents())
        val result = if (conversationId == null || emotion == null || summary.isNullOrBlank()) {
            AppResult.Failure(IllegalStateException("Card input is not ready"))
        } else {
            createCard(conversationId, emotion.character, summary)
        }

        // 이미 만들어진 카드는 다시 만들 수 없다. 재시도를 남기면 영구히 409 다.
        val alreadyExists = (result as? AppResult.Failure)?.throwable is CardAlreadyExistsException
        val card = result.getOrNull()
        reduce {
            state.copy(
                isFinishing = false,
                card = card,
                endedButCardFailed = card == null && !alreadyExists,
            )
        }
        if (card == null) {
            postSideEffect(
                ChatRoomSideEffect.ShowToast(if (alreadyExists) CARD_ALREADY_MADE else CARD_FAILED),
            )
        }
    }

    /** 대기 중인 댓글을 간격을 두고 하나씩 노출한다. */
    private fun startRevealing() {
        revealJob = intent {
            while (state.pendingComments.isNotEmpty()) {
                delay(CommentRevealPolicy.nextGapMillis())
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

    /** 남은 댓글을 기다리지 않고 한꺼번에 붙인다. */
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
        const val END_FAILED = "대화를 끝내지 못했어요"
        const val CARD_FAILED = "카드를 만들지 못했어요. 다시 시도해 주세요."
        const val CARD_ALREADY_MADE = "이 대화의 카드는 이미 만들어졌어요"
    }
}

private fun List<Message>.userContents(): List<String> =
    filter { it.sender == MessageSender.User }.map { it.content }
