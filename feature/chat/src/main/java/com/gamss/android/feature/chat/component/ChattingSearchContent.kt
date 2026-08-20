package com.gamss.android.feature.chat.component

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.modifier.noRippleCombinedClickable
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.feature.chat.ChattingListActions
import com.gamss.android.feature.chat.ChattingListState
import com.gamss.android.feature.chat.R
import com.gamss.android.feature.chat.SearchFailureReason
import com.gamss.android.feature.chat.toSearchFailureReason

@Composable
internal fun ChattingSearchContent(
    state: ChattingListState,
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    actions: ChattingListActions,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit,
    idleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var scrolledGeneration by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(state.search.searchGeneration) {
        if (state.search.hasSearched && state.search.searchGeneration != scrolledGeneration) {
            listState.scrollToItem(0)
            scrolledGeneration = state.search.searchGeneration
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.search.isActive,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            SearchInput(
                keyword = state.search.keyword,
                onKeywordChanged = onKeywordChanged,
                onSearch = onSearch,
                onCancel = onCancel,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (state.search.isActive && state.search.hasSearched) {
                SearchResultContent(
                    chattingRooms = chattingRooms,
                    state = state,
                    actions = actions,
                    listState = listState,
                    onRetry = onSearch,
                )
            } else {
                idleContent()
            }
        }
    }
}

@Composable
private fun SearchInput(
    keyword: TextFieldValue,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 18.dp, end = 18.dp, bottom = 12.dp)),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = keyword,
            onValueChange = onKeywordChanged,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    stringResource(R.string.chatting_list_search_placeholder),
                    style = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray400)
                )
            },
            singleLine = true,
            shape = RectangleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GamssTheme.colors.gray075,
                unfocusedContainerColor = GamssTheme.colors.gray075,
                focusedIndicatorColor = GamssTheme.colors.gray075,
                unfocusedIndicatorColor = GamssTheme.colors.gray075,
                cursorColor = GamssTheme.colors.gray900
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                if (keyword.text.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                role = Role.Button,
                                onClick = { onKeywordChanged(TextFieldValue()) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(GamssIcons.ClearButton),

                            contentDescription = stringResource(
                                R.string.chatting_list_search_clear_content_description,
                            ),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )
        Text(
            text = stringResource(R.string.chatting_list_search_cancel),
            style = GamssTheme.typography.body4Medium,
            color = GamssTheme.colors.gray950,
            modifier = Modifier.noRippleCombinedClickable(
                onClick = {
                    keyboardController?.hide()
                    onCancel()
                },
                onLongClick = null
            ),
        )
    }
}

@Composable
private fun SearchResultContent(
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    state: ChattingListState,
    actions: ChattingListActions,
    listState: LazyListState,
    onRetry: () -> Unit,
) {
    when (val refresh = chattingRooms.loadState.refresh) {
        is LoadState.Loading -> LoadingContent()
        is LoadState.Error -> ErrorContent(
            messageRes = refresh.error.toSearchFailureReason().messageRes,
            onRetry = onRetry,
        )

        is LoadState.NotLoading -> if (chattingRooms.itemCount == 0) {
            MessageContent(messageRes = R.string.chatting_list_search_empty)
        } else {
            SearchResultList(
                chattingRooms = chattingRooms,
                listState = listState,
                state = state,
                actions = actions,
            )
        }
    }
}

@Composable
private fun SearchResultList(
    chattingRooms: LazyPagingItems<ChattingRoomSummary>,
    listState: LazyListState,
    state: ChattingListState,
    actions: ChattingListActions,
) {
    ConversationList(
        chattingRooms = chattingRooms,
        state = state,
        actions = actions,
        listState = listState,
        modifier = Modifier.fillMaxSize(),
        appendContent = {
            when (val append = chattingRooms.loadState.append) {
                is LoadState.Loading -> item { AppendLoadingItem() }
                is LoadState.Error -> item {
                    AppendErrorItem(
                        messageRes = append.error.toSearchFailureReason().messageRes,
                        onRetry = chattingRooms::retry,
                    )
                }

                is LoadState.NotLoading -> Unit
            }
        },
    )
}

@Composable
private fun AppendLoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppendErrorItem(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(messageRes))
        GamssButton(
            label = stringResource(R.string.chatting_list_search_retry),
            onClick = onRetry
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageContent(@StringRes messageRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorContent(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(messageRes))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.chatting_list_search_retry))
        }
    }
}

private val SearchFailureReason.messageRes: Int
    @StringRes get() = when (this) {
        SearchFailureReason.NETWORK -> R.string.chatting_list_search_network_error
        SearchFailureReason.INVALID_INPUT -> R.string.chatting_list_search_invalid_input
        SearchFailureReason.UNKNOWN -> R.string.chatting_list_search_unknown_error
    }
