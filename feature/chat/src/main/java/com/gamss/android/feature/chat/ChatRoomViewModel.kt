package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.config.GetRemoteConfigFlagUseCase
import com.gamss.android.domain.config.RemoteConfigKey
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.nextCommentRevealGapMillis
import com.gamss.android.domain.conversation.takeWithinMessageLimit
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.safety.DetectRiskInTextUseCase
import com.gamss.android.domain.safety.RiskLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

private typealias ChatRoomSyntax = Syntax<ChatRoomState, ChatRoomSideEffect>

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val session: ConversationSession,
    private val tokenUsageRefreshNotifier: TokenUsageRefreshNotifier,
    private val detectRiskInText: DetectRiskInTextUseCase,
    private val getRemoteConfigFlag: GetRemoteConfigFlagUseCase,
) : ViewModel(),
    ContainerHost<ChatRoomState, ChatRoomSideEffect> {

    override val container = container<ChatRoomState, ChatRoomSideEffect>(ChatRoomState())

    /** 구성 변경으로 화면이 다시 그려져도 서버를 다시 부르지 않는다. */
    private var started = false

    @Volatile
    private var revealJob: Job? = null

    fun start(conversationId: Long) {
        if (started) return
        started = true
        loadChatEndFeatureFlag()
        loadMessages(conversationId)
        // 결과를 기다리지 않는다 — 채팅방에 들어온 시점부터 온디바이스 모델 다운로드를 미리
        // 걸어둬 첫 메시지/카드 생성 시점엔 이미 받아져 있을 확률을 높이는 순수 최적화용 호출이다.
        viewModelScope.launch { session.prefetchOnDeviceModels() }
    }

    private fun loadChatEndFeatureFlag() = intent {
        val useChatEndFeature = getRemoteConfigFlag(RemoteConfigKey.UseChatEndFeature)
        reduce { state.copy(useChatEndFeature = useChatEndFeature) }
    }

    private fun loadMessages(conversationId: Long) = intent {
        reduce { state.copy(conversationId = conversationId, isLoading = true) }

        val pending = session.consumePendingReveal(conversationId)
        if (pending != null) {
            reduce {
                state.copy(
                    isLoading = false,
                    messages = listOf(pending.message),
                    pendingComments = pending.comments,
                )
            }
            launchCommentReveal()
            return@intent
        }

        when (val result = session.restore(conversationId)) {
            is AppResult.Success ->
                reduce { state.copy(isLoading = false, messages = result.data, pendingComments = emptyList()) }
            is AppResult.Failure -> {
                reduce { state.copy(isLoading = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(LOAD_FAILED))
            }
        }
    }

    fun onInputChange(text: String) = blockingIntent {
        reduce { state.copy(input = text.takeWithinMessageLimit()) }
    }

    fun onReplyTargetSelect(message: Message) = intent {
        val character = (message.sender as? MessageSender.Character)?.character ?: return@intent
        reduce {
            state.copy(
                replyTarget = ReplyTarget(
                    messageId = message.id,
                    characterName = character.displayName,
                    content = message.content,
                ),
            )
        }
    }

    fun onReplyTargetClear() = intent {
        reduce { state.copy(replyTarget = null) }
    }

    fun onSend() = intent {
        flushPendingComments()

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

        // session.send() 전에 고정된 메시지 내용으로 위험 신호 검사
        val detection = detectRiskInText(sending.content)
        if (detection.level != RiskLevel.NONE) {
            reduce {
                state.copy(
                    riskDetection = detection,
                    // CRITICAL이면 전송을 중단하므로 다시 전송 가능한 상태로 복구
                    isSending = if (detection.shouldBlock) {
                        false
                    } else {
                        state.isSending
                    },
                )
            }

            if (detection.shouldBlock) {
                return@intent
            }
        }

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
                        messages = state.messages + sent.message,
                        pendingComments = sent.comments,
                        input = if (state.input == sending.content) "" else state.input,
                        replyTarget = state.replyTarget.takeIf { it?.messageId != sending.replyToMessageId },
                    )
                }
                launchCommentReveal()
                sent.commentStatus.toUserMessage()?.let { postSideEffect(ChatRoomSideEffect.ShowToast(it)) }
                tokenUsageRefreshNotifier.requestRefresh()
                session.finishSend()
            }
            is AppResult.Failure -> {
                reduce { state.copy(isSending = false) }
                postSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED))
            }
        }
    }

    fun onEndRequest() = intent {
        // 검사와 전환을 한 reduce 안에서 처리해야 연타로 두 번 시작되지 않는다.
        // 전이 여부는 단계 값이 아니라 별도 플래그로 든다. 이미 CreatingCard 인 상태에서 또 시작되는 걸 막는다.
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

    fun onRiskDialogDismiss() = intent {
        reduce { state.copy(riskDetection = null) }
    }

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
