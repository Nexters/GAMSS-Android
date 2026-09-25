package com.gamss.android.feature.chat.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
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
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = GamssTheme.colors.gray075,
        unfocusedContainerColor = GamssTheme.colors.gray075,
        focusedIndicatorColor = GamssTheme.colors.gray075,
        unfocusedIndicatorColor = GamssTheme.colors.gray075,
    )

    BasicTextField(
        value = keyword,
        onValueChange = onKeywordChanged,
        modifier = modifier.defaultMinSize(minHeight = TextFieldDefaults.MinHeight),
        textStyle = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray950),
        cursorBrush = SolidColor(GamssTheme.colors.gray950),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        interactionSource = interactionSource,
    ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = keyword.text,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = {
                Text(
                    stringResource(R.string.chatting_list_search_placeholder),
                    style = GamssTheme.typography.body4Medium.copy(color = GamssTheme.colors.gray400),
                )
            },
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
            shape = RectangleShape,
            colors = colors,
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                top = SearchInputVerticalPadding,
                bottom = SearchInputVerticalPadding,
            ),
        )
    }
}

private val SearchInputVerticalPadding = 11.dp

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
