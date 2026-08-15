package com.gamss.android.feature.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.component.chat.ChatReplyQuote
import com.gamss.android.core.designsystem.component.chat.ChatSender
import com.gamss.android.core.designsystem.component.chat.GamssReceivedChatBubble
import com.gamss.android.core.designsystem.component.chat.GamssSentChatBubble
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.feature.chat.component.SupportAgencyDialog
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * @param onCardClose 카드 시트를 닫을 때 호출한다. 이 화면을 실제로 벗어나야 한다.
 *  머무르면 카드 단계가 그대로라 시트가 다시 열린다.
 * @param showEndButtonInDebug 원격 플래그와 무관하게 debug 빌드에서 종료 흐름을 검증한다.
 */
@Composable
fun ChatRoomScreen(
    conversationId: Long,
    onCardClose: () -> Unit,
    onCardDeleteClick: (Long) -> Unit,
    showEndButtonInDebug: Boolean,
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
        showEndButtonInDebug = showEndButtonInDebug,
        modifier = modifier,
    )

    state.riskDetection?.let { detection ->
        SupportAgencyDialog(
            agencies = detection.agencies,
            onCallClick = { agency -> context.dialOrNotify(agency.phoneNumber) },
            onEmergencyCallClick = { context.dialOrNotify(EMERGENCY_PHONE_NUMBER) },
            // 감정 결과 화면으로 이동하는 별도 계약이 생기기 전까지는 안내만 닫는다.
            onConfirm = viewModel::onRiskDialogDismiss,
            onDismiss = viewModel::onRiskDialogDismiss,
        )
    }

    // else 를 두지 않아야 단계를 추가할 때 화면이 컴파일 에러로 알려준다.
    when (val endFlow = state.endFlow) {
        EndFlow.Confirming -> EndConversationDialog(
            onConfirm = viewModel::onEndConfirm,
            onDismiss = viewModel::onEndCancel,
        )
        is EndFlow.CardReady -> CardBottomSheet(
            card = endFlow.card,
            onDismiss = onCardClose,
            onDeleteClick = { onCardDeleteClick(endFlow.card.id) },
        )
        EndFlow.NotStarted,
        EndFlow.Ending,
        EndFlow.CreatingCard,
        EndFlow.CardFailedRetryable,
        EndFlow.CardFailedFinal,
        -> Unit
    }
}

// 디자인 컴포넌트로 대체하기
@Composable
private fun EndConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_room_end_dialog_title)) },
        text = { Text(stringResource(R.string.chat_room_end_dialog_subTitle)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.chat_room_end_dialog_confirm_button_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_room_end_dialog_dismiss_button_label))
            }
        },
    )
}

// 카드생성 bottomsheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardBottomSheet(
    card: Card,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit,
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
            TextButton(onClick = onDeleteClick) {
                Text(text = stringResource(R.string.chat_room_card_delete_button))
            }
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
    showEndButtonInDebug: Boolean,
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
            // 디자인 컴포넌트 적용 예정
            ChatRoomTopBar(
                endFlow = state.endFlow,
                canEnd = state.canEnd,
                showEndButton = state.useChatEndFeature || showEndButtonInDebug,
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
                    MessageBubble(
                        message = message,
                        messages = state.messages,
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
    showEndButton: Boolean,
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
        if (showEndButton) {
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

/**
 * @param messages 답장 대상 메시지의 내용을 찾기 위한 전체 목록. [Message.repliesToMessageId] 가
 *  가리키는 메시지가 이 목록에 없으면(예: 아직 로드되지 않은 과거 메시지) 답장 인용 없이 표시한다.
 */
@Composable
private fun MessageBubble(
    message: Message,
    messages: List<Message>,
    isReplyTarget: Boolean,
    onCharacterMessageClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val replyQuote = message.repliesToMessageId
        ?.let { targetId -> messages.find { it.id == targetId } }
        ?.toReplyQuote()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isReplyTarget) {
                    Modifier.background(GamssTheme.colors.blue.copy(alpha = 0.12f))
                } else {
                    Modifier
                },
            ),
    ) {
        when (val sender = message.sender) {
            MessageSender.User -> GamssSentChatBubble(
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            is MessageSender.Character -> GamssReceivedChatBubble(
                sender = ChatSender(name = sender.character.displayName),
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onCharacterMessageClick(message) },
            )
            MessageSender.Unknown -> GamssReceivedChatBubble(
                sender = ChatSender(name = ""),
                message = message.content,
                time = message.createdTime,
                replyQuote = replyQuote,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

private fun Message.toReplyQuote(): ChatReplyQuote {
    val label = when (val target = sender) {
        is MessageSender.Character -> "${target.character.displayName}에게 답장"
        MessageSender.User -> "나에게 답장"
        MessageSender.Unknown -> "답장"
    }
    return ChatReplyQuote(senderLabel = label, message = content)
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

private fun Context.dialOrNotify(phoneNumber: String?) {
    if (phoneNumber == null) return
    if (!dial(phoneNumber)) {
        Toast.makeText(
            this,
            getString(R.string.safety_call_unavailable, phoneNumber),
            Toast.LENGTH_LONG,
        ).show()
    }
}

@Suppress("SwallowedException")
private fun Context.dial(phoneNumber: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}")))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

private const val EMERGENCY_PHONE_NUMBER = "119"
