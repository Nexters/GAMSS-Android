package com.gamss.android.feature.chat

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gamss.android.core.common.util.formatConversationDate
import com.gamss.android.core.designsystem.component.GamssScrollToBottomButton
import com.gamss.android.core.designsystem.component.GamssTokenUsageTooltip
import com.gamss.android.core.designsystem.modifier.addFocusCleaner
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationHeight
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationHorizontalPadding
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIcon
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIconAction
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationTitleAlignment
import com.gamss.android.core.ui.chat.ChatMessageBubble
import com.gamss.android.core.ui.chat.rememberReplyQuoteLookup
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.chat.component.CardFoldOverlay
import com.gamss.android.domain.repository.TokenUsageAlert
import com.gamss.android.feature.chat.component.EndConversationDialog
import com.gamss.android.feature.chat.component.LoadingMessageBubble
import com.gamss.android.feature.chat.component.MessageInputBar
import com.gamss.android.feature.chat.component.NewMessageToast
import com.gamss.android.feature.chat.component.SupportAgencyDialog
import com.gamss.android.feature.chat.util.AnimatedChatMessage
import com.gamss.android.feature.chat.util.ChatMessageAnimation
import com.gamss.android.feature.chat.util.ChatScrollState
import com.gamss.android.feature.chat.util.dialOrNotify
import com.gamss.android.feature.chat.util.rememberChatMessageAnimationState
import com.gamss.android.feature.chat.util.rememberChatScrollState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import kotlin.coroutines.cancellation.CancellationException
import java.time.LocalDate

/**
 * 두 콜백 모두 이 화면을 실제로 벗어나야 한다. 머무르면 카드 단계가 그대로라 접기 연출이 다시 열린다.
 *
 * @param onCardDiscard 접은 카드를 통에 버린 뒤 호출한다. 버린 카드가 쌓인 보관함 칸으로 보내려면
 *  어느 감정 칸인지, 그중 어느 날 카드인지 알아야 하므로 함께 넘긴다.
 * @param onCardSkip 연출을 건너뛴 뒤 호출한다. 카드는 이미 기록에 남아 결과는 같지만, 버리는
 *  동작을 하지 않았으니 보관함까지 데려가지 않는다.
 */
@Composable
fun ChatRoomScreen(
    conversationId: Long,
    onCardDiscard: (EmotionCharacter, LocalDate) -> Unit,
    onCardSkip: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(conversationId) { viewModel.start(conversationId) }

    // 토큰 알림은 화면이 보이는 동안만 구독해야 다른 채팅방이 가로채지 않는다. Nav3의
    // SinglePaneSceneStrategy는 다른 방으로 넘어가면 이 컴포저블을 ON_PAUSE 이벤트 없이 그냥
    // 컴포지션에서 제거한다 — LifecycleEventEffect(ON_PAUSE)는 이 경우 못 잡아서(dispose 시
    // 콜백을 안 부른다), dispose 시에도 정리 콜백이 보장되는 LifecycleResumeEffect를 쓴다.
    LifecycleResumeEffect(conversationId) {
        viewModel.onScreenResumed()
        onPauseOrDispose { viewModel.onScreenPaused() }
    }

    val tokenUsageLowMessage = stringResource(R.string.chat_room_token_usage_low_toast)
    val tokenUsageExhaustedMessage = stringResource(R.string.chat_room_token_usage_exhausted_toast)

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChatRoomSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()

            is ChatRoomSideEffect.ShowTokenUsageAlert -> {
                val message = when (sideEffect.alert) {
                    TokenUsageAlert.LOW -> tokenUsageLowMessage
                    TokenUsageAlert.EXHAUSTED -> tokenUsageExhaustedMessage
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val actions = remember(viewModel) {
        ChatRoomActions(
            onInputChange = viewModel::onInputChange,
            onSendClick = viewModel::onSend,
            onCharacterMessageClick = viewModel::onReplyTargetSelect,
            onReplyTargetClear = viewModel::onReplyTargetClear,
            onEndClick = viewModel::onEndRequest,
            onTokenUsageToggle = viewModel::onTokenUsageToggle,
            onTokenUsageRetry = viewModel::onTokenUsageRetry
        )
    }

    ChatRoomContent(
        state = state,
        actions = actions,
        modifier = modifier,
        onBackClick = onBackClick,
    )

    state.riskDetection?.let { detection ->
        SupportAgencyDialog(
            agencies = detection.agencies,
            onCallClick = { agency -> context.dialOrNotify(agency.phoneNumber) },
            onEmergencyCallClick = { context.dialOrNotify(EMERGENCY_PHONE_NUMBER) },
            onDismiss = viewModel::onRiskDialogDismiss,
        )
    }

    ChatRoomEndFlowHost(
        endFlow = state.endFlow,
        onEndConfirm = viewModel::onEndConfirm,
        onEndCancel = viewModel::onEndCancel,
        onFoldTap = viewModel::onCardFoldTap,
        onCardSkip = onCardSkip,
        onCardDiscard = onCardDiscard,
    )
}

/** 대화 종료 단계마다 위에 얹히는 창. 카드가 어느 보관함 칸으로 가는지도 여기서 뽑아낸다. */
@Composable
private fun ChatRoomEndFlowHost(
    endFlow: EndFlow,
    onEndConfirm: () -> Unit,
    onEndCancel: () -> Unit,
    onFoldTap: () -> Unit,
    onCardSkip: () -> Unit,
    onCardDiscard: (EmotionCharacter, LocalDate) -> Unit,
) {
    // else 를 두지 않아야 단계를 추가할 때 화면이 컴파일 에러로 알려준다.
    when (endFlow) {
        EndFlow.Confirming -> EndConversationDialog(
            onConfirm = onEndConfirm,
            onDismiss = onEndCancel,
        )

        is EndFlow.CardReady -> CardFoldOverlay(
            card = endFlow.card,
            foldStage = endFlow.foldStage,
            onFoldTap = onFoldTap,
            onSkip = onCardSkip,
            onDiscard = { onCardDiscard(endFlow.card.character, endFlow.card.date) },
        )

        EndFlow.NotStarted,
        EndFlow.Ending,
        EndFlow.CreatingCard,
        EndFlow.CardFailedRetryable,
        EndFlow.CardFailedFinal,
        -> Unit
    }
}

@Immutable
private data class ChatRoomActions(
    val onInputChange: (String) -> Unit,
    val onSendClick: () -> Unit,
    val onCharacterMessageClick: (Message) -> Unit,
    val onReplyTargetClear: () -> Unit,
    val onEndClick: () -> Unit,
    val onTokenUsageToggle: () -> Unit,
    val onTokenUsageRetry: () -> Unit,
)

@Composable
private fun ChatRoomContent(
    state: ChatRoomState,
    actions: ChatRoomActions,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val messageAnimationState = rememberChatMessageAnimationState(
        conversationId = state.conversationId,
        isLoading = state.isLoading,
        messageIds = state.messages.map(Message::id),
    )
    val chatScrollState = rememberChatScrollState(state = state, listState = listState)

    // WindowInsets.ime 게터 자체가 @Composable이라 LaunchedEffect(코루틴) 안에서 직접 부를 수
    // 없다. 여기서 객체 참조만 한 번 얻어두면, 이후 getBottom() 호출은 일반 함수 호출이라 코루틴
    // 안에서도 매번 최신 값을 읽을 수 있다.
    val imeInsets = WindowInsets.ime
    val imeDensity = LocalDensity.current
    val imeBottomPx = imeInsets.getBottom(imeDensity)

    LaunchedEffect(listState, imeInsets, imeDensity) {
        var previous = imeInsets.getBottom(imeDensity)
        snapshotFlow { imeInsets.getBottom(imeDensity) }.collect { current ->
            val delta = current - previous
            previous = current
            if (delta != 0) {
                try {
                    listState.scrollBy(delta.toFloat())
                } catch (_: CancellationException) {
                    currentCoroutineContext().ensureActive()
                }
            }
        }
    }

    // 키보드가 오르내리는 동안엔 위 보정 스크롤이 인셋 변화를 프레임 단위로 정확히 따라잡지
    // 못해 listState.canScrollForward 가 순간적으로 흔들리고, 그 값을 그대로 따르는 스크롤
    // 버튼이 깜빡인다. imeBottomPx 가 바뀔 때마다 이 이펙트가 재시작되므로(진행 중이던 delay는
    // 취소됨), 인셋이 한동안(마지막 변화 후 IME_SETTLE_GRACE_PERIOD_MILLIS) 안 바뀌어 안정됐을
    // 때만 버튼을 노출해 깜빡임을 없앤다.
    var isImeInTransition by remember { mutableStateOf(false) }
    LaunchedEffect(imeBottomPx) {
        isImeInTransition = true
        delay(IME_SETTLE_GRACE_PERIOD_MILLIS)
        isImeInTransition = false
    }

    Scaffold(
        modifier = modifier.addFocusCleaner(focusManager),
        topBar = { ChatRoomTopBar(state = state, actions = actions, onBackClick = onBackClick) },
        // 상위 Scaffold 가 인셋을 이미 적용해, imePadding 을 그대로 쓰면 이중 적용된다.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GamssTheme.colors.background)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        ) {
            ChatMessageList(
                state = state,
                actions = actions,
                listState = listState,
                animationState = messageAnimationState,
                scrollState = chatScrollState,
                showScrollToBottomButton = chatScrollState.showScrollToBottomButton && !isImeInTransition,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            ChatRoomInputSection(
                endFlow = state.endFlow,
                input = state.input,
                isInputEnabled = !state.isLoading && state.endFlow == EndFlow.NotStarted,
                isSending = state.isSending,
                isTokenExhausted = state.isTokenExhausted,
                replyTarget = state.replyTarget,
                actions = actions,
            )
        }
    }
}

@Composable
private fun ChatRoomTopBar(
    state: ChatRoomState,
    actions: ChatRoomActions,
    onBackClick: () -> Unit,
) {
    Box {
        GamssTopNavigation(
            title = state.conversationCreatedAt?.let(::formatConversationDate).orEmpty(),
            titleAlignment = GamssTopNavigationTitleAlignment.Center,
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
            rightActions = listOfNotNull(
                when {
                    state.endFlow.isBusy -> GamssTopNavigationIconAction(
                        icon = GamssTopNavigationIcon.CreateCard,
                        onClick = {},
                        isLoading = true,
                    )
                    state.canEnd -> GamssTopNavigationIconAction(
                        icon = GamssTopNavigationIcon.CreateCard,
                        onClick = actions.onEndClick,
                    )
                    else -> null
                },
                GamssTopNavigationIconAction(
                    icon = GamssTopNavigationIcon.CheckToken,
                    onClick = actions.onTokenUsageToggle,
                ),
            ),
        )

        if (state.isTokenUsagePopupExpanded) {
            TokenUsagePopup(
                usagePercent = state.tokenUsagePercent,
                isLoading = state.isTokenUsageLoading,
                onDismissRequest = actions.onTokenUsageToggle,
                onRetryClick = actions.onTokenUsageRetry,
            )
        }
    }
}

/**
 * 상단 CheckToken 아이콘 아래에 뜬다. 오프셋은 [GamssTopNavigation]이 공개한 크기 상수로
 * 계산한다 — [GamssCharacterPicker] 를 입력바 아래에 띄울 때 쓰는 것과 같은 방식이다.
 */
@Composable
private fun TokenUsagePopup(
    usagePercent: Int?,
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val popupOffset = with(LocalDensity.current) {
        IntOffset(
            x = -GamssTopNavigationHorizontalPadding.roundToPx(),
            y = GamssTopNavigationHeight.roundToPx(),
        )
    }
    Popup(
        alignment = Alignment.TopEnd,
        offset = popupOffset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        GamssTokenUsageTooltip(
            title = stringResource(R.string.chat_room_token_usage_title),
            usagePercent = usagePercent,
            usagePercentLabel = if (usagePercent != null) {
                stringResource(
                    R.string.chat_room_token_usage_percent,
                    usagePercent,
                )
            } else {
                ""
            },
            resetTimeLabel = stringResource(R.string.chat_room_token_usage_reset_time),
            failMessage = stringResource(R.string.chat_room_check_token_usage_fail),
            retryLabel = stringResource(R.string.chat_room_check_token_usage_button_label),
            onRetryClick = onRetryClick,
            isLoading = isLoading,
        )
    }
}

@Composable
private fun ChatMessageList(
    state: ChatRoomState,
    actions: ChatRoomActions,
    listState: LazyListState,
    animationState: ChatMessageAnimation,
    scrollState: ChatScrollState,
    showScrollToBottomButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val replyQuotes = rememberReplyQuoteLookup(state.messages)

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = "오늘 어떤 일이 있었나요?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(state.messages, key = { it.id }) { message ->
                val replyQuote = replyQuotes.quoteFor(message)
                AnimatedChatMessage(
                    messageId = message.id,
                    shouldAnimate = animationState.shouldAnimate(message.id),
                    listState = listState,
                ) {
                    ChatMessageBubble(
                        message = message,
                        replyQuote = replyQuote,
                        onCharacterMessageClick = actions.onCharacterMessageClick,
                    )
                }
            }
            if (state.isAwaitingComments) {
                val nextCharacter = (state.pendingComments.firstOrNull()?.sender as? MessageSender.Character)
                    ?.character
                item { LoadingMessageBubble(character = nextCharacter) }
            }
        }

        scrollState.newMessageToast?.let { toastMessage ->
            NewMessageToast(
                message = toastMessage,
                onClick = { scrollState.dismissToastAndScrollToBottom(state) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }

        if (showScrollToBottomButton) {
            GamssScrollToBottomButton(
                onClick = { scrollState.scrollToBottom(state) },
                contentDescription = stringResource(R.string.chat_room_scroll_to_bottom),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ChatRoomInputSection(
    endFlow: EndFlow,
    input: String,
    isInputEnabled: Boolean,
    isSending: Boolean,
    isTokenExhausted: Boolean,
    replyTarget: ReplyTarget?,
    actions: ChatRoomActions,
    modifier: Modifier = Modifier,
) {
    if (endFlow is EndFlow.Ended) {
        Text(
            text = "끝난 대화예요. 새 대화를 시작해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        return
    }

    MessageInputBar(
        input = input,
        enabled = isInputEnabled,
        isSending = isSending,
        isTokenExhausted = isTokenExhausted,
        replyTarget = replyTarget,
        onInputChange = actions.onInputChange,
        onSendClick = actions.onSendClick,
        onReplyClear = actions.onReplyTargetClear,
        modifier = Modifier,
    )
}

private const val EMERGENCY_PHONE_NUMBER = "119"
private const val IME_SETTLE_GRACE_PERIOD_MILLIS = 120L

@Preview(name = "Light", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChatRoomLightPreview() {
    GamssTheme(darkTheme = false) {
        ChatRoomPreviewContent()
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChatRoomDarkPreview() {
    GamssTheme(darkTheme = true) {
        ChatRoomPreviewContent()
    }
}

@Composable
private fun ChatRoomPreviewContent() {
    val messages = listOf(
        Message(
            id = 1,
            conversationId = 1,
            sender = MessageSender.Character(EmotionCharacter.JOY),
            content = "안녕! 오늘도 행복한 하루~!",
            createdTime = "오후 1:38",
        ),
        Message(
            id = 2,
            conversationId = 1,
            sender = MessageSender.User,
            content = "안녕하세요 반가워요",
            createdTime = "오후 1:39",
        ),
        Message(
            id = 3,
            conversationId = 1,
            sender = MessageSender.Character(EmotionCharacter.SADNESS),
            content = "오늘은 좀 힘든 하루였어요",
            repliesToMessageId = 2,
            createdTime = "오후 1:40",
        ),
    )
    val state = ChatRoomState(
        conversationId = 1,
        messages = messages,
        input = "",
        replyTarget = ReplyTarget(
            messageId = 1,
            characterName = "기쁨",
            content = "안녕! 오늘도 행복한 하루~!",
        ),
    )
    val actions = ChatRoomActions(
        onInputChange = {},
        onSendClick = {},
        onCharacterMessageClick = {},
        onReplyTargetClear = {},
        onEndClick = {},
        onTokenUsageToggle = {},
        onTokenUsageRetry = {}
    )
    ChatRoomContent(
        state = state,
        actions = actions,
        onBackClick = {},
    )
}
