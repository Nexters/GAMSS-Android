package com.gamss.android.feature.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.component.GamssCharacterPicker
import com.gamss.android.core.designsystem.component.GamssCharacterPickerItem
import com.gamss.android.core.designsystem.component.GamssDisclosureToggle
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
import com.gamss.android.domain.emotion.EmotionCharacter
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun HomeScreen(
    onNavigateToSetting: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is HomeSideEffect.NavigateToSetting -> onNavigateToSetting()
            is HomeSideEffect.OpenConversation -> onOpenConversation(sideEffect.conversationId)
            is HomeSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
        }
    }

    val actions = remember(viewModel) {
        HomeActions(
            onSettingClick = viewModel::navigateToSetting,
            onInputChange = viewModel::onInputChange,
            onSubmit = viewModel::onSubmit,
            onEmotionPickerToggle = viewModel::onEmotionPickerToggle,
            onEmotionPickerDismiss = viewModel::onEmotionPickerDismiss,
            onEmotionToggle = viewModel::onEmotionToggle,
        )
    }

    // Popup 은 별도 창이라 뒤로가기를 받지 못한다. 화면 쪽에서 직접 닫아 준다.
    // BackHandler 는 dispatcher 소유자를 요구해 프리뷰에서 터지므로 HomeContent 밖에 둔다.
    BackHandler(enabled = state.isEmotionPickerExpanded, onBack = actions.onEmotionPickerDismiss)

    HomeContent(state = state, actions = actions, modifier = modifier)
}

@Immutable
private data class HomeActions(
    val onSettingClick: () -> Unit,
    val onInputChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onEmotionPickerToggle: () -> Unit,
    val onEmotionPickerDismiss: () -> Unit,
    val onEmotionToggle: (EmotionCharacter) -> Unit,
)

@Composable
private fun HomeContent(
    state: HomeState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // 상위 Scaffold 가 내비게이션 바를 이미 소비했다. 남은 키보드 높이만 피한다.
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
            .dismissOnTapOutside(actions.onEmotionPickerDismiss),
    ) {
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

            HomeInputSection(state = state, actions = actions)

            Spacer(modifier = Modifier.weight(GREETING_BOTTOM_WEIGHT))
        }
    }
}

@Composable
private fun HomeInputSection(
    state: HomeState,
    actions: HomeActions,
) {
    val pickerItems = remember(state.selectedCharacters) { state.selectedCharacters.toPickerItems() }
    var inputBarHeightPx by remember { mutableIntStateOf(0) }

    // 패널은 Popup 으로 띄운다. 흐름에 넣으면 펼칠 때마다 인사말과 입력바가 위로 밀린다.
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = InputBarHorizontalPadding)) {
        GamssInputBar(
            value = state.input,
            onValueChange = actions.onInputChange,
            onSend = actions.onSubmit,
            placeholder = stringResource(R.string.home_input_placeholder),
            sendContentDescription = stringResource(R.string.home_input_submit_description),
            enabled = !state.isSending,
            beforeSendSlot = {
                GamssDisclosureToggle(
                    label = stringResource(R.string.home_character_picker),
                    expanded = state.isEmotionPickerExpanded,
                    onClick = actions.onEmotionPickerToggle,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { inputBarHeightPx = it.height },
        )

        if (state.isEmotionPickerExpanded) {
            val pickerOffset = with(LocalDensity.current) {
                // 입력창 높이가 100dp/149dp+ 로 달라지므로 겹침 폭만 고정해 실측 높이 기준으로 잡는다.
                IntOffset(
                    x = PickerHorizontalInset.roundToPx(),
                    y = (inputBarHeightPx.toDp() - PickerOverlapHeight).roundToPx(),
                )
            }
            Popup(
                alignment = Alignment.TopEnd,
                offset = pickerOffset,
                onDismissRequest = actions.onEmotionPickerDismiss,
            ) {
                GamssCharacterPicker(items = pickerItems, onToggle = actions.onEmotionToggle)
            }
        }
    }
}

/**
 * 입력창 바깥을 누르면 포커스를 내려놓아야 collapsed 로 돌아가고 키보드도 닫힌다. 캐릭터 패널은
 * 포커스와 무관한 별도 창이라 [onDismiss] 로 같이 닫는다.
 */
@Composable
private fun Modifier.dismissOnTapOutside(onDismiss: () -> Unit): Modifier {
    val focusManager = LocalFocusManager.current
    // clickable 을 쓰면 레이블 없는 클릭 노드가 화면 전체 크기로 시맨틱 트리에 들어간다.
    return pointerInput(onDismiss) {
        detectTapGestures {
            focusManager.clearFocus()
            onDismiss()
        }
    }
}

private fun Set<EmotionCharacter>.toPickerItems(): List<GamssCharacterPickerItem<EmotionCharacter>> =
    EmotionCharacter.entries.map { character ->
        GamssCharacterPickerItem(
            value = character,
            label = character.displayName,
            selected = character in this,
        )
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

// Figma 입력창은 402dp 화면에서 366dp 폭이므로 좌우 여백은 각각 18dp다.
private val InputBarHorizontalPadding: Dp = 18.dp

private val PickerHorizontalInset = (-61).dp

// 100dp/149dp 두 입력창 높이 모두에서 Figma가 보여 준 9~10dp 겹침 폭.
private val PickerOverlapHeight = 9.dp

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

private fun previewActions() = HomeActions(
    onSettingClick = {},
    onInputChange = {},
    onSubmit = {},
    onEmotionPickerToggle = {},
    onEmotionPickerDismiss = {},
    onEmotionToggle = {},
)
