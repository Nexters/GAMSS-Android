package com.gamss.android.feature.chat

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ChattingListScreen(
    onChatClick: (conversationId: Long?) -> Unit,
    viewModel: ChattingListViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ChattingListSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            Text(
                text = "채팅",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onChatClick(null) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "새 대화")
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.conversations.isEmpty() -> Text(
                    text = "오늘 나눈 대화가 없어요",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn {
                    items(state.conversations, key = { it.id }) { conversation ->
                        ListItem(
                            headlineContent = { Text(conversation.title ?: NO_TITLE) },
                            modifier = Modifier.clickable { onChatClick(conversation.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private const val NO_TITLE = "제목 없는 대화"
