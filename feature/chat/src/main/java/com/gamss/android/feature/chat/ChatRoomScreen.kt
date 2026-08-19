package com.gamss.android.feature.chat

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.common.util.formatConversationDate
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssTokenUsageTooltip
import com.gamss.android.core.designsystem.modifier.gamssShadow
import com.gamss.android.core.designsystem.snackbar.GamssSnackBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationHeight
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationHorizontalPadding
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIcon
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIconAction
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationTitleAlignment
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.repository.TokenUsageAlert
import com.gamss.android.feature.chat.component.EndConversationDialog
import com.gamss.android.feature.chat.component.LoadingMessageBubble
import com.gamss.android.feature.chat.component.MessageBubble
import com.gamss.android.feature.chat.component.MessageInputBar
import com.gamss.android.feature.chat.component.NewMessageToast
import com.gamss.android.feature.chat.component.SupportAgencyDialog
import com.gamss.android.feature.chat.component.toReplyQuote
import com.gamss.android.feature.chat.util.AnimatedChatMessage
import com.gamss.android.feature.chat.util.ChatMessageAnimation
import com.gamss.android.feature.chat.util.ChatScrollState
import com.gamss.android.feature.chat.util.dialOrNotify
import com.gamss.android.feature.chat.util.rememberChatMessageAnimationState
import com.gamss.android.feature.chat.util.rememberChatScrollState
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * @param onCardClose 카드 시트를 닫을 때 호출한다. 이 화면을 실제로 벗어나야 한다.
 *  머무르면 카드 단계가 그대로라 시트가 다시 열린다.
 */
@Composable
fun ChatRoomScreen(
    conversationId: Long,
    onCardClose: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(conversationId) { viewModel.start(conversationId) }

    // GamssSnackBar는 Scaffold(ChatRoomContent) 안에 있지만, 사이드이펙트 수집은 여기서 하는 게
    // Toast와 한곳에 모여 흐름을 따라가기 쉽다. 그래서 host만 여기서 만들어 아래로 넘긴다.
    val snackbarHostState = remember { SnackbarHostState() }
    val tokenUsageLowMessage = stringResource(R.string.chat_room_token_usage_low_snackbar)
    val tokenUsageExhaustedMessage = stringResource(R.string.chat_room_token_usage_exhausted_snackbar)

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChatRoomSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()

            is ChatRoomSideEffect.ShowTokenUsageAlert -> {
                val message = when (sideEffect.alert) {
                    TokenUsageAlert.LOW -> tokenUsageLowMessage
                    TokenUsageAlert.EXHAUSTED -> tokenUsageExhaustedMessage
                }

                snackbarHostState.showSnackbar(
                    TokenUsageSnackbarVisuals(message = message, alert = sideEffect.alert),
                )
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
        snackbarHostState = snackbarHostState,
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

    // else 를 두지 않아야 단계를 추가할 때 화면이 컴파일 에러로 알려준다.
    when (val endFlow = state.endFlow) {
        EndFlow.Confirming -> EndConversationDialog(
            onConfirm = viewModel::onEndConfirm,
            onDismiss = viewModel::onEndCancel,
        )

        is EndFlow.CardReady -> CardBottomSheet(card = endFlow.card, onDismiss = onCardClose)
        EndFlow.NotStarted,
        EndFlow.Ending,
        EndFlow.CreatingCard,
        EndFlow.CardFailedRetryable,
        EndFlow.CardFailedFinal,
        -> Unit
    }
}

// 카드생성 bottomsheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardBottomSheet(
    card: Card,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = card.character.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = card.summary, style = MaterialTheme.typography.titleMedium)
            Text(
                text = card.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

/**
 * [alert] 종류에 따라 GamssSnackBar 아이콘을 달리 그리기 위한 커스텀 [SnackbarVisuals].
 * 기본 `showSnackbar(message)` 오버로드는 문자열만 실어 나를 수 있어, 렌더 시점에 아이콘을
 * 고를 근거가 메시지 문구밖에 안 남는다 — 문구가 바뀌면 아이콘도 같이 깨지므로 이 타입으로 감싼다.
 */
private data class TokenUsageSnackbarVisuals(
    override val message: String,
    val alert: TokenUsageAlert,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * 알림 종류별 GamssSnackBar 아이콘. LOW는 두 가지 색(원+체크)이 들어 있어 tint 하지 않고,
 * EXHAUSTED(전부 소진)는 경고 색으로 강조해 심각도 차이를 보여준다.
 */
@Composable
private fun TokenUsageAlertIcon(alert: TokenUsageAlert?) {
    when (alert) {
        TokenUsageAlert.EXHAUSTED -> Icon(
            painter = painterResource(GamssIcons.Alert),
            contentDescription = null,
            tint = GamssTheme.colors.red,
        )
        TokenUsageAlert.LOW, null -> Icon(
            painter = painterResource(GamssIcons.Alert),
            contentDescription = null,
            tint = GamssTheme.colors.yellow,
        )
    }
}

@Composable
private fun ChatRoomContent(
    state: ChatRoomState,
    actions: ChatRoomActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val messageAnimationState = rememberChatMessageAnimationState(
        conversationId = state.conversationId,
        isLoading = state.isLoading,
        messageIds = state.messages.map(Message::id),
    )
    val chatScrollState = rememberChatScrollState(state = state, listState = listState)

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    var previousImeBottomPx by remember { mutableIntStateOf(imeBottomPx) }
    LaunchedEffect(imeBottomPx) {
        val delta = imeBottomPx - previousImeBottomPx
        previousImeBottomPx = imeBottomPx
        if (delta != 0) {
            listState.scrollBy(delta.toFloat())
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
        modifier = modifier,
        topBar = { ChatRoomTopBar(state = state, actions = actions, onBackClick = onBackClick) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val alert = (data.visuals as? TokenUsageSnackbarVisuals)?.alert
                GamssSnackBar(
                    message = data.visuals.message,
                    snackBarIcon = { TokenUsageAlertIcon(alert) },
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        },
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
                    !state.useChatEndFeature -> null
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
    // 리스트 전체(state.messages)를 각 아이템에 그대로 넘기면, 메시지가 하나 추가될 때마다 리스트
    // 참조가 바뀌어 이미 떠 있던 다른 모든 말풍선까지 재구성 대상이 된다. 답장 대상 조회를 여기서
    // 한 번에 끝내고 아이템별로는 결과값(replyQuote)만 넘기면, 안 바뀐 아이템은 재구성을 건너뛸 수
    // 있다. LazyListScope 빌더 본문은 @Composable이 아니라 여기(바깥)서 remember해야 한다.
    val messagesById = remember(state.messages) { state.messages.associateBy(Message::id) }

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
                val replyQuote = message.repliesToMessageId
                    ?.let { targetId -> messagesById[targetId] }
                    ?.toReplyQuote()
                AnimatedChatMessage(
                    messageId = message.id,
                    shouldAnimate = animationState.shouldAnimate(message.id),
                    listState = listState,
                ) {
                    MessageBubble(
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
            ScrollToBottomButton(
                onClick = { scrollState.scrollToBottom(state) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ScrollToBottomButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .gamssShadow(shape = CircleShape)
            .clip(CircleShape)
            .background(GamssTheme.colors.gray700, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(GamssIcons.ScrollDown),
            contentDescription = stringResource(R.string.chat_room_scroll_to_bottom),
            modifier = Modifier.size(24.dp),
            tint = GamssTheme.colors.gray025,
        )
    }
}

@Composable
private fun ChatRoomInputSection(
    endFlow: EndFlow,
    input: String,
    isInputEnabled: Boolean,
    isSending: Boolean,
    replyTarget: ReplyTarget?,
    actions: ChatRoomActions,
) {
    if (endFlow is EndFlow.Ended) {
        Text(
            text = "끝난 대화예요. 새 대화를 시작해 보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        return
    }

    MessageInputBar(
        input = input,
        enabled = isInputEnabled,
        isSending = isSending,
        replyTarget = replyTarget,
        onInputChange = actions.onInputChange,
        onSendClick = actions.onSendClick,
        onReplyClear = actions.onReplyTargetClear,
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
        useChatEndFeature = true,
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
        snackbarHostState = remember { SnackbarHostState() },
        onBackClick = {},
    )
}
