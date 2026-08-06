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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.domain.conversation.MAX_MESSAGE_LENGTH
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ChatRoomScreen(
    conversationId: Long?,
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
        )
    }

    ChatRoomContent(state = state, actions = actions)
}

private class ChatRoomActions(
    val onInputChange: (String) -> Unit,
    val onSendClick: () -> Unit,
    val onCharacterMessageClick: (Message) -> Unit,
    val onReplyTargetClear: () -> Unit,
)

@Composable
private fun ChatRoomContent(
    state: ChatRoomState,
    actions: ChatRoomActions,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isAwaitingComments) {
        val itemCount = state.messages.size + if (state.isAwaitingComments) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Scaffold(
        topBar = {
            Text(
                text = "대화",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
        },
        // 시스템 인셋은 GamssNavHost 의 Scaffold 가 이미 적용한다.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // imePadding 을 그대로 쓰면 navigation bar 높이만큼 이중 적용된다.
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

            ChatRoomInputSection(state = state, actions = actions)
        }
    }
}

@Composable
private fun ChatRoomInputSection(
    state: ChatRoomState,
    actions: ChatRoomActions,
) {
    state.replyTarget?.let { replyTarget ->
        ReplyTargetBanner(replyTarget = replyTarget, onClear = actions.onReplyTargetClear)
    }

    MessageInputBar(
        input = state.input,
        canSend = state.canSend,
        onInputChange = actions.onInputChange,
        onSendClick = actions.onSendClick,
    )
}

@Composable
private fun MessageBubble(
    message: Message,
    isReplyTarget: Boolean,
    onCharacterMessageClick: (Message) -> Unit,
) {
    val character = (message.sender as? MessageSender.Character)?.character

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (character == null) Alignment.End else Alignment.Start,
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
            color = if (character == null) {
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
                .widthIn(max = 280.dp)
                .then(
                    if (character == null) {
                        Modifier
                    } else {
                        Modifier.clickable { onCharacterMessageClick(message) }
                    },
                ),
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
private fun GeneratingIndicator() {
    Row(
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
