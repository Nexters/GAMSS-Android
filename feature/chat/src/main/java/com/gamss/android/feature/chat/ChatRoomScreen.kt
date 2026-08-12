package com.gamss.android.feature.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * @param initialMessage 홈에서 적어 온 첫 걱정. 새 대화로 들어올 때만 채워진다.
 * @param onCardClose 카드 시트를 닫을 때 호출한다. 이 화면을 실제로 벗어나야 한다.
 *  머무르면 카드 단계가 그대로라 시트가 다시 열린다.
 */
@Composable
fun ChatRoomScreen(
    conversationId: Long?,
    onCardClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialMessage: String? = null,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    // ViewModel 의 중복 전송 가드는 프로세스가 죽으면 함께 사라진다. 반면 NavKey 는 복원되므로
    // 소비 여부를 화면 저장 상태에 남겨야 같은 문구가 새 대화로 한 번 더 나가지 않는다.
    var initialMessageConsumed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.start(conversationId, initialMessage.takeUnless { initialMessageConsumed })
        initialMessageConsumed = true
    }

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

    ChatRoomContent(state = state, actions = actions, modifier = modifier)

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

@Composable
private fun EndConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("대화를 끝낼까요?") },
        text = { Text("끝내면 이 대화에 메시지를 더 보낼 수 없고, 감정 카드가 만들어져요.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("끝내기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

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
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isAwaitingComments) {
        val itemCount = state.messages.size + if (state.isAwaitingComments) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ChatRoomTopBar(
                endFlow = state.endFlow,
                canEnd = state.canEnd,
                onEndClick = actions.onEndClick,
            )
        },
        // 상위 Scaffold 가 인셋을 이미 적용해, imePadding 을 그대로 쓰면 이중 적용된다.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    MessageBubble(
                        message = message,
                        isReplyTarget = state.replyTarget?.messageId == message.id,
                        onCharacterMessageClick = actions.onCharacterMessageClick,
                    )
                }
                if (state.isAwaitingComments) {
                    item { GeneratingIndicator() }
                }
            }

            HorizontalDivider()

            ChatRoomInputSection(
                endFlow = state.endFlow,
                input = state.input,
                canSend = state.canSend,
                replyTarget = state.replyTarget,
                actions = actions,
            )
        }
    }
}

@Composable
private fun ChatRoomTopBar(
    endFlow: EndFlow,
    canEnd: Boolean,
    onEndClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "대화",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (endFlow.isBusy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onEndClick, enabled = canEnd) {
                Text(
                    when {
                        endFlow == EndFlow.CardFailedRetryable -> "카드 다시 만들기"
                        endFlow is EndFlow.Ended -> "끝난 대화"
                        else -> "대화 끝내기"
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatRoomInputSection(
    endFlow: EndFlow,
    input: String,
    canSend: Boolean,
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

    replyTarget?.let {
        ReplyTargetBanner(replyTarget = it, onClear = actions.onReplyTargetClear)
    }

    MessageInputBar(
        input = input,
        canSend = canSend,
        onInputChange = actions.onInputChange,
        onSendClick = actions.onSendClick,
    )
}

@Composable
private fun MessageBubble(
    message: Message,
    isReplyTarget: Boolean,
    onCharacterMessageClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val character = (message.sender as? MessageSender.Character)?.character
    val isFromUser = message.sender is MessageSender.User
    val isCharacterMessage = character != null

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start,
    ) {
        if (character != null) {
            Text(
                text = character.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
            )
        } else if (message.repliesToMessageId != null) {
            Text(
                text = "↩ 답장",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp, end = 4.dp),
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = if (isReplyTarget) {
                BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = Modifier
                .widthIn(max = BubbleMaxWidth)
                .then(if (isCharacterMessage) Modifier.clickable { onCharacterMessageClick(message) } else Modifier),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
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

@Composable
private fun ReplyTargetBanner(
    replyTarget: ReplyTarget,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "↩ ${replyTarget.characterName}에게 답장",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClear) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "답장 취소")
        }
    }
}

@Composable
private fun MessageInputBar(
    input: String,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("지금 기분을 적어보세요") },
            supportingText = { Text("${input.length}/$MAX_MESSAGE_LENGTH") },
            maxLines = 4,
        )
        IconButton(onClick = onSendClick, enabled = canSend) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "보내기")
        }
    }
}

private val BubbleMaxWidth = 280.dp
