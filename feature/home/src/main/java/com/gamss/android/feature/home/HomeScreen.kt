package com.gamss.android.feature.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.component.GamssCharacterPicker
import com.gamss.android.core.designsystem.component.GamssCharacterPickerItem
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
import com.gamss.android.domain.conversation.takeWithinMessageLimit
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
            onMessageLengthExceeded = viewModel::onMessageLengthExceeded,
            onSubmit = viewModel::onSubmit,
            onEmotionPickerToggle = viewModel::onEmotionPickerToggle,
            onEmotionToggle = viewModel::onEmotionToggle,
        )
    }

    HomeContent(state = state, actions = actions, modifier = modifier)
}

@Immutable
private data class HomeActions(
    val onSettingClick: () -> Unit,
    val onInputChange: (String) -> Unit,
    val onMessageLengthExceeded: () -> Unit,
    val onSubmit: () -> Unit,
    val onEmotionPickerToggle: () -> Unit,
    val onEmotionToggle: (EmotionCharacter) -> Unit,
)

@Composable
private fun HomeContent(
    state: HomeState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().dismissKeyboardOnTapOutside()) {
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

    // 한 번 알린 뒤에는 계속 이어 쳐도 다시 알리지 않고, 길이를 줄였다 다시 넘길 때만 알린다.
    var lengthWarned by remember { mutableStateOf(false) }

    // 패널은 Popup 으로 띄운다. 흐름에 넣으면 펼칠 때마다 인사말과 입력바가 위로 밀린다.
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = InputBarHorizontalPadding)) {
        GamssInputBar(
            value = state.input,
            // ViewModel도 같은 도메인 정책으로 방어하지만, IME 입력 순간에도 140자를 넘겨
            // 화면에 보이지 않도록 UI 경계에서 먼저 제한한다.
            onValueChange = {
                val limited = it.takeWithinMessageLimit()
                if (limited != it) {
                    if (!lengthWarned) {
                        actions.onMessageLengthExceeded()
                        lengthWarned = true
                    }
                } else {
                    lengthWarned = false
                }
                actions.onInputChange(limited)
            },
            onTrailingClick = actions.onSubmit,
            placeholder = stringResource(R.string.home_input_placeholder),
            trailingContentDescription = stringResource(R.string.home_input_submit_description),
            enabled = !state.isSending,
            trailingAction = {
                CharacterPickerToggle(
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
                // 입력창 높이가 64dp/149dp+ 로 달라지므로 겹침 폭만 고정해 실측 높이 기준으로 잡는다.
                IntOffset(
                    x = PickerHorizontalInset.roundToPx(),
                    y = (inputBarHeightPx.toDp() - PickerOverlapHeight).roundToPx(),
                )
            }
            Popup(
                alignment = Alignment.TopEnd,
                offset = pickerOffset,
                onDismissRequest = actions.onEmotionPickerToggle,
            ) {
                GamssCharacterPicker(items = pickerItems, onToggle = actions.onEmotionToggle)
            }
        }
    }
}

/** 입력창 바깥을 누르면 포커스를 내려놓아야 collapsed 로 돌아가고 키보드도 닫힌다. */
@Composable
private fun Modifier.dismissKeyboardOnTapOutside(): Modifier {
    val focusManager = LocalFocusManager.current
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { focusManager.clearFocus() },
    )
}

@Composable
private fun CharacterPickerToggle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        // collapsed 에서만 48dp 까지 넓어진다. expanded 컨트롤 행은 32dp 로 고정 측정돼 그만큼만 잡힌다.
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.DropdownList, onClick = onClick)
            .padding(horizontal = PickerToggleHitPadding, vertical = PickerToggleVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PickerToggleGap),
    ) {
        GamssText(
            text = stringResource(R.string.home_character_picker),
            style = GamssTheme.typography.body4Medium,
            // 흰 입력바 위에서 다크 테마의 흰 전경색이 사라지지 않게 Figma 전경색을 유지한다.
            color = GamssTheme.colors.black,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(GamssIcons.RightChevron),
            contentDescription = null,
            tint = GamssTheme.colors.black,
            // 아래위 셰브론 에셋이 없어 오른쪽 셰브론을 돌려 쓴다. 글리프가 뷰포트 중심에서 벗어나 있어
            // 회전축을 글리프 자신의 중심으로 옮긴다. 그러지 않으면 펼칠 때마다 위아래로 튄다.
            modifier = Modifier
                .size(PickerToggleChevronSize)
                .graphicsLayer {
                    rotationZ = if (expanded) -CHEVRON_ROTATION else CHEVRON_ROTATION
                    transformOrigin = TransformOrigin(CHEVRON_CENTER_X, CHEVRON_CENTER_Y)
                },
        )
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

// 64dp/149dp 두 입력창 높이 모두에서 Figma가 보여 준 9~10dp 겹침 폭.
private val PickerOverlapHeight = 9.dp
private val PickerToggleGap = 4.dp
private val PickerToggleHitPadding = 4.dp

// SVG 입력바 내부 높이에 맞춰 라벨의 시각 높이만 사용한다.
private val PickerToggleVerticalPadding = 0.dp
private val PickerToggleChevronSize = 16.dp
private const val CHEVRON_ROTATION = 90f

// ic_right_chevron 글리프의 실제 중심. 24 뷰포트에서 stroke 포함 x 12.4~21.3, y 3.9~20.2 다.
private const val CHEVRON_CENTER_X = 16.85f / 24f
private const val CHEVRON_CENTER_Y = 12.03f / 24f

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
    onMessageLengthExceeded = {},
    onSubmit = {},
    onEmotionPickerToggle = {},
    onEmotionToggle = {},
)
