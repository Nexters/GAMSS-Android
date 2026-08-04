package com.gamss.android.feature.chatSearch

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.domain.chat.ChattingRoomSummary
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private const val LOAD_MORE_THRESHOLD = 3

@Composable
fun SearchChattingScreen(
    viewModel: SearchChattingViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SearchChattingSideEffect.ShowToast -> {
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SearchChattingContent(
        state = state,
        onKeywordChanged = viewModel::onKeywordChanged,
        onSearch = viewModel::search,
        onLoadNextPage = viewModel::loadNextPage,
    )
}

@Composable
fun SearchChattingContent(
    state: SearchChattingState,
    onKeywordChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadNextPage: () -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.rooms.size, state.canLoadMore, state.isAppending, state.isLoading) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            state.canLoadMore &&
                !state.isAppending &&
                !state.isLoading &&
                lastVisibleIndex >= state.rooms.lastIndex - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadNextPage()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "채팅방 검색",
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.keyword,
                        onValueChange = onKeywordChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("검색어") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    )
                    Button(
                        onClick = onSearch,
                        enabled = state.canSearch,
                    ) {
                        Text("검색")
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingContent(innerPadding)
            state.hasSearched && state.rooms.isEmpty() -> EmptyContent(innerPadding)
            else -> SearchResultList(
                rooms = state.rooms,
                isAppending = state.isAppending,
                listState = listState,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun SearchResultList(
    rooms: List<ChattingRoomSummary>,
    isAppending: Boolean,
    listState: LazyListState,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        items(
            items = rooms,
            key = { it.conversationId },
        ) { room ->
            ListItem(
                headlineContent = { Text(room.title) },
                supportingContent = { Text("${room.status} · ${room.createdAt}") },
            )
            HorizontalDivider()
        }

        if (isAppending) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "검색 결과가 없어요",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
