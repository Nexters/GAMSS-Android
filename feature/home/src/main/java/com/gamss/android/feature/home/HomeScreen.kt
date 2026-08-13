package com.gamss.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.component.GamssIconButton
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.core.designsystem.component.GamssInputBar
import com.gamss.android.core.designsystem.component.GamssLogo
import com.gamss.android.core.designsystem.component.GamssMarkerHighlight
import com.gamss.android.core.designsystem.component.GamssPaperSlip
import com.gamss.android.core.designsystem.component.GamssStickyNote
import com.gamss.android.core.designsystem.component.GamssTape
import com.gamss.android.core.designsystem.component.GamssText
import com.gamss.android.core.designsystem.component.GamssTopBar
import com.gamss.android.core.designsystem.theme.GamssTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun HomeScreen(
    onNavigateToSetting: () -> Unit,
    onStartConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is HomeSideEffect.NavigateToSetting -> onNavigateToSetting()
            is HomeSideEffect.StartConversation -> onStartConversation(sideEffect.message)
        }
    }

    val actions = remember(viewModel) {
        HomeActions(
            onSettingClick = viewModel::navigateToSetting,
            onInputChange = viewModel::onInputChange,
            onSubmit = viewModel::onSubmit,
        )
    }

    HomeContent(state = state, actions = actions, modifier = modifier)
}

@Immutable
private data class HomeActions(
    val onSettingClick: () -> Unit,
    val onInputChange: (String) -> Unit,
    val onSubmit: () -> Unit,
)

@Composable
private fun HomeContent(
    state: HomeState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        HomeDecorations()

        Column(modifier = Modifier.fillMaxSize()) {
            GamssTopBar(
                contentPadding = PaddingValues(start = HeaderStartPadding, end = HeaderEndPadding),
                leading = { GamssLogo(contentDescription = stringResource(R.string.home_logo_description)) },
                trailing = {
                    GamssIconButton(
                        iconRes = GamssIcons.Setting,
                        contentDescription = stringResource(R.string.home_setting_description),
                        onClick = actions.onSettingClick,
                    )
                },
            )

            Spacer(modifier = Modifier.weight(GREETING_TOP_WEIGHT))

            HomeGreeting(
                nickname = state.nickname,
                modifier = Modifier
                    .padding(start = GreetingStartPadding)
                    // 닉네임이 도착하기 전에 먼저 그리면 문구가 옆으로 밀린다. 자리만 잡아 두고 감춘다.
                    .alpha(if (state.isLoading) 0f else 1f),
            )

            Spacer(modifier = Modifier.height(GreetingToInputGap))

            GamssInputBar(
                value = state.input,
                onValueChange = actions.onInputChange,
                onTrailingClick = actions.onSubmit,
                placeholder = stringResource(R.string.home_input_placeholder),
                trailingContentDescription = stringResource(R.string.home_input_submit_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = InputBarHorizontalPadding),
            )

            Spacer(modifier = Modifier.weight(GREETING_BOTTOM_WEIGHT))
        }
    }
}

@Composable
private fun HomeGreeting(
    nickname: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(GreetingWordGap),
        ) {
            if (nickname != null) {
                GamssMarkerHighlight {
                    GamssText(
                        text = stringResource(R.string.home_greeting_nickname, nickname),
                        style = GamssTheme.typography.pixelTitle2,
                    )
                }
            }
            GamssText(
                text = stringResource(R.string.home_greeting_suffix),
                style = GamssTheme.typography.pixelTitle2,
            )
        }
        GamssText(
            text = stringResource(R.string.home_greeting_question),
            style = GamssTheme.typography.pixelTitle2,
        )
    }
}

/**
 * 좌표는 Figma 402x874 프레임에서 상태바 높이(49)를 뺀 값이다. 콘텐츠 흐름에 끼어들면 안 되므로 절대 배치로 얹고,
 * 화면이 좁을 때 잘리는 건 [com.gamss.android.core.designsystem.component.GamssPaperBackground] 의 clip 에 맡긴다.
 */
@Composable
private fun BoxScope.HomeDecorations() {
    GamssStickyNote(
        text = stringResource(R.string.home_sample_note),
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = 244.dp, y = 114.73.dp),
    )
    GamssTape(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = 211.dp, y = 494.dp),
    )
    GamssPaperSlip(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = 45.dp, y = 574.dp),
    )
}

// 헤더 아래(111) ~ 인사말(281), 입력바 아래(418) ~ 탭바 위(769) 간격 비율을 그대로 옮긴 값.
private const val GREETING_TOP_WEIGHT = 170f
private const val GREETING_BOTTOM_WEIGHT = 351f

private val HeaderStartPadding = 20.dp

// 설정 버튼은 터치 영역을 12dp 넓혀 두었다. 아이콘이 화면 끝에서 20dp 에 놓이도록 그만큼 뺀다.
private val HeaderEndPadding = 8.dp
private val GreetingStartPadding = 27.dp
private val GreetingWordGap = 6.dp
private val GreetingToInputGap = 22.dp
private val InputBarHorizontalPadding: Dp = 20.dp

@Preview(name = "Home - Light", showBackground = true, widthDp = 402, heightDp = 720)
@Composable
@Suppress("UnusedPrivateMember")
private fun HomeContentLightPreview() {
    GamssTheme(darkTheme = false) {
        HomeContent(state = previewState(), actions = previewActions())
    }
}

@Preview(name = "Home - Dark", showBackground = true, widthDp = 402, heightDp = 720)
@Composable
@Suppress("UnusedPrivateMember")
private fun HomeContentDarkPreview() {
    GamssTheme(darkTheme = true) {
        HomeContent(state = previewState(), actions = previewActions())
    }
}

@Preview(name = "Home - No nickname", showBackground = true, widthDp = 402, heightDp = 720)
@Composable
@Suppress("UnusedPrivateMember")
private fun HomeContentWithoutNicknamePreview() {
    GamssTheme {
        HomeContent(
            state = HomeState(isLoading = false, nickname = null, input = "오늘 발표가 너무 떨려요"),
            actions = previewActions(),
        )
    }
}

private fun previewState() = HomeState(isLoading = false, nickname = "이소연")

private fun previewActions() = HomeActions(onSettingClick = {}, onInputChange = {}, onSubmit = {})
