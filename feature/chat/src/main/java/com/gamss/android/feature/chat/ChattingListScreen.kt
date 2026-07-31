package com.gamss.android.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private data class DummyChat(
    val name: String,
    val lastMessage: String,
)

private val dummyChats = List(10) {
    DummyChat(name = "채팅방 ${it + 1}", lastMessage = "임시 메시지 내용입니다.")
}

@Composable
fun ChattingListScreen(
    onChatClick: () -> Unit,
    viewModel: ChattingListViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    viewModel.collectSideEffect { }

    Scaffold(
        topBar = {
            Text(
                text = "채팅",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(dummyChats) { chat ->
                    ListItem(
                        headlineContent = { Text(chat.name) },
                        supportingContent = { Text(chat.lastMessage) },
                        modifier = Modifier.clickable(onClick = onChatClick),
                    )
                    Divider()
                }
            }
        }
    }
}
