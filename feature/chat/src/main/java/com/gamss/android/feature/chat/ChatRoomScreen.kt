package com.gamss.android.feature.chat

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.common.util.formatConversationDate
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.component.GamssInputBar
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.dialog.GamssDialog
import com.gamss.android.core.designsystem.dialog.GamssDialogAction
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIcon
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationIconAction
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigationTitleAlignment
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.chat.component.EndConversationDialog
import com.gamss.android.feature.chat.component.MessageBubble
import com.gamss.android.feature.chat.component.MessageInputBar
import com.gamss.android.feature.chat.component.SupportAgencyDialog
import com.gamss.android.feature.chat.util.AnimatedChatMessage
import com.gamss.android.feature.chat.util.ChatMessageAnimation
import com.gamss.android.feature.chat.util.dialOrNotify
import com.gamss.android.feature.chat.util.rememberChatMessageAnimationState
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.LocalDateTime

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

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChatRoomSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
        }
    }

    val actions = remember(viewModel) {
        ChatRoomActions(
            onInputChange = viewModel::onInputChange,
            onSendClick = viewModel::onSend,
            onCharacterMessageClick = viewModel::onReplyTargetSelect,
            onReplyTargetClear = viewModel::onReplyTargetClear,
            onEndClick = viewModel::onEndRequest,
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
)

@Composable
private fun ChatRoomContent(
    state: ChatRoomState,
    actions: ChatRoomActions,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val messageAnimationState = rememberChatMessageAnimationState(
        conversationId = state.conversationId,
        isLoading = state.isLoading,
        messageIds = state.messages.map(Message::id),
    )

    LaunchedEffect(state.messages.size, state.isAwaitingComments) {
        val itemCount = state.messages.size + if (state.isAwaitingComments) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    var previousImeBottomPx by remember { mutableIntStateOf(imeBottomPx) }
    LaunchedEffect(imeBottomPx) {
        val delta = imeBottomPx - previousImeBottomPx
        previousImeBottomPx = imeBottomPx
        if (delta != 0) {
            listState.scrollBy(delta.toFloat())
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            GamssTopNavigation(
                title = state.conversationCreatedAt?.let(::formatConversationDate).orEmpty(),
                titleAlignment = GamssTopNavigationTitleAlignment.Center,
                showLeftIcon = true,
                onLeftIconClick = onBackClick,
                rightActions = listOfNotNull(
                    if (state.useChatEndFeature) {
                        if (!state.endFlow.isBusy && state.canEnd) {
                            GamssTopNavigationIconAction(
                                icon = GamssTopNavigationIcon.CreateCard,
                                onClick = actions.onEndClick,
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    },
                    GamssTopNavigationIconAction(icon = GamssTopNavigationIcon.Menu, onClick = {
                        // 토큰 확인 페이지?
                    }),
                ),
            )
        },
        // 상위 Scaffold 가 인셋을 이미 적용해, imePadding 을 그대로 쓰면 이중 적용된다.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GamssTheme.colors.background)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
        ) {
            ChatMessageList(
                state = state,
                actions = actions,
                listState = listState,
                animationState = messageAnimationState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            ChatRoomInputSection(
                endFlow = state.endFlow,
                input = state.input,
                isInputEnabled = !state.isLoading && state.endFlow == EndFlow.NotStarted,
                replyTarget = state.replyTarget,
                actions = actions,
            )
        }
    }
}

@Composable
private fun ChatMessageList(
    state: ChatRoomState,
    actions: ChatRoomActions,
    listState: LazyListState,
    animationState: ChatMessageAnimation,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
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
            AnimatedChatMessage(
                messageId = message.id,
                shouldAnimate = animationState.shouldAnimate(message.id),
                listState = listState,
            ) {
                MessageBubble(
                    message = message,
                    messages = state.messages,
                    onCharacterMessageClick = actions.onCharacterMessageClick,
                )
            }
        }
        if (state.isAwaitingComments) {
            item { GeneratingIndicator() }
        }
    }
}

@Composable
private fun ChatRoomInputSection(
    endFlow: EndFlow,
    input: String,
    isInputEnabled: Boolean,
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
        replyTarget = replyTarget,
        onInputChange = actions.onInputChange,
        onSendClick = actions.onSendClick,
        onReplyClear = actions.onReplyTargetClear,
    )
}

@Composable
private fun GeneratingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Text(
            text = "답장을 쓰고 있어요",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val EMERGENCY_PHONE_NUMBER = "119"


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
    )
    ChatRoomContent(state = state, actions = actions, onBackClick = {})
}
