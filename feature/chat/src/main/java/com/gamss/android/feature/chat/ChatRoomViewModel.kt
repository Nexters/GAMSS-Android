package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.nextCommentRevealGapMillis
import com.gamss.android.domain.conversation.takeWithinMessageLimit
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.safety.DetectRiskInTextUseCase
import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.usecase.GetDailyTokenUsageUseCase
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
    private val getDailyTokenUsageUseCase: GetDailyTokenUsageUseCase,
) : ViewModel(),
    ContainerHost<ChatRoomState, ChatRoomSideEffect> {

    override val container = container<ChatRoomState, ChatRoomSideEffect>(ChatRoomState())

    /** 구성 변경으로 화면이 다시 그려져도 서버를 다시 부르지 않는다. */
    private var started = false

    @Volatile
    private var revealJob: Job? = null

    @Volatile
    private var tokenAlertJob: Job? = null

    fun start(conversationId: Long) {
        if (started) return
        started = true
        loadMessages(conversationId)
        observeTokenExhausted()
        // 방에 들어오자마자 소진 여부를 알아야 전송 버튼을 처음부터 올바르게 잠글 수 있다 —
        // 이전에 이미 소진된 채로 재입장한 경우, 한 번도 안 보내봤어도 버튼이 바로 잠겨야 한다.
        tokenUsageRefreshNotifier.requestRefresh()
        // 결과를 기다리지 않는다 — 채팅방에 들어온 시점부터 온디바이스 모델 다운로드를 미리
        // 걸어둬 첫 메시지/카드 생성 시점엔 이미 받아져 있을 확률을 높이는 순수 최적화용 호출이다.
        viewModelScope.launch { session.prefetchOnDeviceModels() }
    }

    /**
     * [tokenUsageRefreshNotifier]의 알림(LOW/EXHAUSTED)을 토스트로 옮긴다. EXHAUSTED를 여기서
     * 다루는 이유: [CommentGenerationStatus.LIMIT_EXCEEDED]는 "이미 소진된 채로 보냈다"는
     * 신호라 막 소진된 순간을 놓친다 — 실측(exceeded)을 보는 이 스트림이 유일한 소스다(아래
     * [onSend] 참고).
     *
     * alerts는 Channel이라 값을 그 순간 receive() 중인 구독자 한 곳에만 준다. 백스택에 남은
     * 다른 채팅방이 먼저 가로채지 않도록, [start]가 아니라 화면이 RESUMED일 때만 구독한다.
     */
    fun onScreenResumed() {
        if (tokenAlertJob?.isActive == true) return
        tokenAlertJob = intent {
            tokenUsageRefreshNotifier.alerts.collect { alert ->
                postSideEffect(ChatRoomSideEffect.ShowTokenUsageAlert(alert))
            }
        }
    }

    fun onScreenPaused() {
        tokenAlertJob?.cancel()
        tokenAlertJob = null
    }

    private fun observeTokenExhausted() = intent {
        tokenUsageRefreshNotifier.isExhausted.collect { exhausted ->
            reduce {
                state.copy(
                    isTokenExhausted = exhausted,
                    input = if (exhausted) "" else state.input,
                )
            }
        }
    }

    private fun loadMessages(conversationId: Long) = intent {
        reduce { state.copy(conversationId = conversationId, isLoading = true) }

        val pending = session.consumePendingReveal(conversationId)
        if (pending != null) {
            reduce {
                state.copy(
                    isLoading = false,
                    conversationCreatedAt = pending.createdAt,
                    messages = listOf(pending.sent.message),
                    pendingComments = pending.sent.comments,
                )
            }
            launchCommentReveal()
            return@intent
        }

        when (val result = session.restore(conversationId)) {
            is AppResult.Success ->
                reduce {
                    state.copy(
                        isLoading = false,
                        conversationCreatedAt = result.data.conversation.createdAt,
                        messages = result.data.messages,
                        pendingComments = emptyList(),
                    )
                }
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

    fun onTokenUsageToggle() = intent {
        var opened = false
        var needsFetch = false
        reduce {
            opened = !state.isTokenUsagePopupExpanded
            needsFetch = opened && state.tokenUsagePercent == null
            state.copy(isTokenUsagePopupExpanded = opened)
        }
        if (needsFetch) refreshTokenUsage()
    }

    fun onTokenUsageRetry() = intent {
        refreshTokenUsage()
    }

    /**
     * 사용자가 직접 요청한 조회(팝업 열기·재시도)는 실패를 그대로 반영해야 재시도 UI가 뜬다.
     * [onSend]의 백그라운드 갱신처럼 조용히 이전 값을 유지하는 fallback을 여기선 쓰지 않는다.
     * 팝업이 조회 중임을 알 수 있도록 요청 전후로 isTokenUsageLoading을 함께 반영한다.
     */
    private suspend fun ChatRoomSyntax.refreshTokenUsage() {
        reduce { state.copy(isTokenUsageLoading = true) }
        val percent = fetchTokenUsagePercent()
        reduce { state.copy(tokenUsagePercent = percent, isTokenUsageLoading = false) }
    }

    /**
     * [onSend] 성공 직후의 백그라운드 갱신. 메시지를 반영하는 reduce와 분리된 별도 인텐트로
     * 띄워, 사용량 조회가 느려도 이미 도착한 메시지 표시가 지연되지 않게 한다. 실패 시엔
     * 이전 값을 조용히 유지한다.
     */
    private fun refreshTokenUsageInBackground() {
        intent {
            val usagePercent = fetchTokenUsagePercent()
            reduce { state.copy(tokenUsagePercent = usagePercent ?: state.tokenUsagePercent) }
        }
    }

    /**
     * 조회 실패는 채팅 자체를 막을 이유가 없어 화면에는 조용히 null 로만 반영한다.
     */
    private suspend fun fetchTokenUsagePercent(): Int? =
        when (val result = getDailyTokenUsageUseCase()) {
            is AppResult.Success -> {
                result.data.usagePercent
            }
            is AppResult.Failure -> {
                null
            }
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
            // 서버 왕복이 끝날 때까지 기다리지 않고 버튼을 누른 즉시 입력칸을 비운다. 아래
            // 위험 신호 차단·전송 실패 분기에서 되돌리지 않는 한 이 상태로 남는다.
            if (pending == null) state else state.copy(isSending = true, input = "")
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
                    // 방금 비운 입력칸을 되돌린다. 그 사이 사용자가 새로 타이핑했다면 덮어쓰지 않는다.
                    input = if (detection.shouldBlock && state.input.isEmpty()) sending.content else state.input,
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
                tokenUsageRefreshNotifier.requestRefresh()
                reduce {
                    state.copy(
                        isSending = false,
                        conversationId = sent.message.conversationId,
                        messages = state.messages + sent.message,
                        pendingComments = sent.comments,
                        replyTarget = state.replyTarget.takeIf { it?.messageId != sending.replyToMessageId },
                    )
                }
                launchCommentReveal()
                sent.commentStatus.toSideEffect()?.let { postSideEffect(it) }
                session.finishSend()
                refreshTokenUsageInBackground()
            }
            is AppResult.Failure -> {
                reduce {
                    state.copy(
                        isSending = false,
                        // 방금 비운 입력칸을 되돌린다. 그 사이 사용자가 새로 타이핑했다면 덮어쓰지 않는다.
                        input = if (state.input.isEmpty()) sending.content else state.input,
                    )
                }
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

    fun onCardFoldTap() = intent {
        reduce {
            val ready = state.endFlow as? EndFlow.CardReady ?: return@reduce state
            val next = ready.foldStage.next ?: return@reduce state
            state.copy(endFlow = ready.copy(foldStage = next))
        }
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

    // LIMIT_EXCEEDED는 토큰이 이미 소진됐을 때 나는 신호라, onSend() 성공 분기에서 이미 호출하는
    // requestRefresh()가 트리거하는 tokenUsageRefreshNotifier의 EXHAUSTED 알림
    // (observeTokenUsageAlerts 참고)이 대신 안내한다 — 토스트를 따로 띄우지 않는다.
    private fun CommentGenerationStatus.toSideEffect(): ChatRoomSideEffect? = when (this) {
        CommentGenerationStatus.DONE, CommentGenerationStatus.LIMIT_EXCEEDED -> null
        CommentGenerationStatus.FAILED -> ChatRoomSideEffect.ShowToast("답장을 받지 못했어요. 잠시 후 다시 보내볼까요?")
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
