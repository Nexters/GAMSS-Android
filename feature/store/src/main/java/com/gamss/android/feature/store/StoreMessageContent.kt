package com.gamss.android.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.theme.GamssTheme

private val ContentWidth = 237.dp

/**
 * 상품 목록 대신 보여주는 안내. [com.gamss.android.core.designsystem.component.GamssNetworkErrorContent]와
 * 같은 배치·타이포를 쓰되 일러스트는 두지 않는다. [actionLabel]과 [onAction]을 함께 주면 하단 버튼을 보여준다.
 */
@Composable
internal fun StoreMessageContent(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(ContentWidth),
            verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing600),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GamssText(
                    text = title,
                    style = GamssTheme.typography.subtitle2,
                    color = GamssTheme.colors.gray900,
                    textAlign = TextAlign.Center,
                )
                if (description != null) {
                    GamssText(
                        text = description,
                        style = GamssTheme.typography.body4Regular,
                        color = GamssTheme.colors.gray600,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (actionLabel != null && onAction != null) {
                GamssButton(
                    label = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "Store message - empty", showBackground = true, widthDp = 402, heightDp = 400)
@Composable
@Suppress("UnusedPrivateMember")
private fun StoreMessageContentEmptyPreview() {
    GamssTheme(darkTheme = false) {
        StoreMessageContent(title = "구매할 수 있는 토큰이 없습니다.", modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Store message - load fail", showBackground = true, widthDp = 402, heightDp = 400)
@Composable
@Suppress("UnusedPrivateMember")
private fun StoreMessageContentLoadFailPreview() {
    GamssTheme(darkTheme = false) {
        StoreMessageContent(
            title = "상품을 불러오지 못했어요.",
            description = "잠시 후 다시 시도해 주세요.",
            actionLabel = "재시도",
            onAction = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
