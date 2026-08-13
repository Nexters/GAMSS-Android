package com.gamss.android.feature.webview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation

/**
 * [GamssWebPage] 하나를 전체 화면으로 여는 공통 웹뷰 화면.
 *
 * 도메인 호출이 없어 상태가 전부 View 계층에 머무르므로 ViewModel을 두지 않는다.
 */
@Composable
fun WebViewScreen(
    page: GamssWebPage,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberGamssWebViewState()
    val handleBack: () -> Unit = {
        if (state.canGoBack()) state.goBack() else onBackClick()
    }

    BackHandler(onBack = handleBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background),
    ) {
        GamssTopNavigation(
            title = stringResource(page.titleRes),
            showLeftIcon = true,
            onLeftIconClick = handleBack,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            GamssWebView(
                url = page.url,
                state = state,
                modifier = Modifier.fillMaxSize(),
            )

            when {
                state.hasError -> WebViewErrorContent(
                    onRetryClick = state::reload,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GamssTheme.colors.background)
                        // 덮은 웹뷰로 터치가 새면 보이지 않는 링크가 눌린다.
                        .pointerInput(Unit) {},
                )

                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun WebViewErrorContent(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(GamssTheme.spacing.spacing300),
        verticalArrangement = Arrangement.spacedBy(
            space = GamssTheme.spacing.spacing300,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.webview_error_message),
            style = GamssTheme.typography.body3Medium,
            color = GamssTheme.colors.gray700,
            textAlign = TextAlign.Center,
        )
        GamssButton(
            label = stringResource(R.string.webview_retry),
            onClick = onRetryClick,
        )
    }
}
