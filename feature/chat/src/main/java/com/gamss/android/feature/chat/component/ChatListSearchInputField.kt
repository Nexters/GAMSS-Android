package com.gamss.android.feature.chat.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.chat.R

/**
 * 채팅 목록 검색창.
 */
@Composable
internal fun ChatListSearchInputField(
    keyword: TextFieldValue,
    onKeywordChanged: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(GamssTheme.colors.gray075, RectangleShape)
            .padding(
                horizontal = SearchInputHorizontalPadding,
                vertical = SearchInputVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(SearchInputIconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = keyword,
            onValueChange = onKeywordChanged,
            modifier = Modifier.weight(1f),
            textStyle = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray950),
            cursorBrush = SolidColor(GamssTheme.colors.gray950),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (keyword.text.isEmpty()) {
                        Text(
                            stringResource(R.string.chatting_list_search_placeholder),
                            style = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray400),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (keyword.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(SearchInputClearIconSize)
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
                    modifier = Modifier.size(SearchInputClearIconSize),
                )
            }
        }
    }
}

private val SearchInputHorizontalPadding = 16.dp
private val SearchInputVerticalPadding = 11.dp
private val SearchInputIconGap = 8.dp
private val SearchInputClearIconSize = 20.dp

@Preview(name = "Placeholder", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChatListSearchInputFieldPlaceholderPreview() {
    GamssTheme(darkTheme = false) {
        ChatListSearchInputField(
            keyword = TextFieldValue(),
            onKeywordChanged = {},
            onSearch = {},
        )
    }
}

@Preview(name = "Typed", showBackground = true)
@Suppress("UnusedPrivateMember")
@Composable
private fun ChatListSearchInputFieldTypedPreview() {
    GamssTheme(darkTheme = false) {
        ChatListSearchInputField(
            keyword = TextFieldValue("검색어"),
            onKeywordChanged = {},
            onSearch = {},
        )
    }
}
