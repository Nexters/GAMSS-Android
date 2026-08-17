package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme

@Preview(name = "TopBar", showBackground = true, widthDp = 402)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssTopBarPreview() {
    GamssTheme {
        GamssTopBar(
            leading = { GamssLogo() },
            trailing = {
                GamssIconButton(
                    iconRes = GamssIcons.Setting,
                    contentDescription = null,
                    onClick = {},
                )
            },
        )
    }
}

@Preview(name = "InputBar - Empty", showBackground = true, widthDp = 402)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssInputBarEmptyPreview() {
    GamssTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            GamssInputBar(
                value = "",
                onValueChange = {},
                onSend = {},
                placeholder = "무슨 이야기를 버려볼까요?",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "InputBar - Filled", showBackground = true, widthDp = 402)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssInputBarFilledPreview() {
    GamssTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            GamssInputBar(
                value = "오늘 발표가 너무 떨려요",
                onValueChange = {},
                onSend = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "BottomBar", showBackground = true, widthDp = 402)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssBottomBarPreview() {
    GamssTheme {
        GamssBottomBar(
            items = listOf(
                GamssBottomBarItem(value = "archive", iconRes = GamssIcons.TabArchive, label = "보관함"),
                GamssBottomBarItem(value = "home", iconRes = GamssIcons.TabHome, label = "홈"),
                GamssBottomBarItem(value = "chat", iconRes = GamssIcons.TabChat, label = "대화"),
            ),
            selectedValue = "home",
            onItemClick = {},
        )
    }
}

@Preview(name = "Decorations", showBackground = true, widthDp = 402, heightDp = 260)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssDecorationsPreview() {
    GamssTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GamssStickyNote(text = "내 귀에 도청장치")
            GamssTape()
            GamssPaperSlip()
        }
    }
}

@Preview(name = "MarkerHighlight", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssMarkerHighlightPreview() {
    GamssTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            GamssMarkerHighlight {
                GamssText(text = "이소연님", style = GamssTheme.typography.pixelTitle2)
            }
        }
    }
}

@Preview(name = "CharacterPicker", showBackground = true)
@Preview(name = "CharacterPicker fontScale 1.3", showBackground = true, fontScale = 1.3f)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssCharacterPickerPreview() {
    GamssTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            GamssCharacterPicker(
                items = listOf("분노", "엉뚱", "까칠", "기쁨", "슬픔", "불안").mapIndexed { index, label ->
                    GamssCharacterPickerItem(value = label, label = label, selected = index < 4)
                },
                onToggle = {},
            )
        }
    }
}
