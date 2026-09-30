package com.gamss.android.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.theme.GamssTheme

private val ContentWidth = 237.dp
private const val TAPE_ALPHA = 0.3f

@Composable
fun GamssNetworkErrorContent(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(ContentWidth),
            verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing600),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing500),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NetworkErrorIllustration()
                Column(
                    verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing100),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GamssText(
                        text = stringResource(R.string.gamss_network_error_title),
                        style = GamssTheme.typography.subtitle2,
                        color = GamssTheme.colors.gray900,
                        textAlign = TextAlign.Center,
                    )
                    GamssText(
                        text = stringResource(R.string.gamss_network_error_description),
                        style = GamssTheme.typography.body4Regular,
                        color = GamssTheme.colors.gray600,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            GamssButton(
                label = stringResource(R.string.gamss_network_error_refresh),
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// VectorDrawable 은 그룹 알파가 없어, 겹친 두 도형인 테이프를 따로 그려 한 번에 투명도를 준다.
@Composable
private fun NetworkErrorIllustration() {
    Box {
        Image(
            painter = painterResource(R.drawable.img_network_error),
            contentDescription = null,
        )
        Image(
            painter = painterResource(R.drawable.img_network_error_tape),
            contentDescription = null,
            modifier = Modifier.graphicsLayer(
                alpha = TAPE_ALPHA,
                compositingStrategy = CompositingStrategy.Offscreen,
            ),
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 402, heightDp = 600)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssNetworkErrorContentLightPreview() {
    GamssTheme(darkTheme = false) {
        GamssNetworkErrorContent(onRefresh = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 402,
    heightDp = 600,
)
@Suppress("UnusedPrivateMember")
@Composable
private fun GamssNetworkErrorContentDarkPreview() {
    GamssTheme(darkTheme = true) {
        GamssNetworkErrorContent(onRefresh = {}, modifier = Modifier.fillMaxSize())
    }
}
